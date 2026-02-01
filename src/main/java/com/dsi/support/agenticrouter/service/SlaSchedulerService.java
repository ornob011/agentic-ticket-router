package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaSchedulerService {

    private final SupportTicketRepository supportTicketRepository;
    private final PolicyConfigRepository policyConfigRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void checkCustomerResponseTimeout() {
        log.info("Checking customer response timeouts");

        int slaHours = PolicyConfigKey.getIntValue(
            policyConfigRepository.findByConfigKeyAndActiveTrue(PolicyConfigKey.SLA_CUSTOMER_RESPONSE_HOURS)
                                  .map(PolicyConfig::getConfigValue)
                                  .orElseThrow(DataNotFoundException::new),
            48
        );

        Instant slaThreshold = Instant.now().minusSeconds(slaHours * 3600L);

        List<SupportTicket> waitingTickets = supportTicketRepository.findByStatusInAndLastActivityAtBefore(
            EnumSet.of(
                TicketStatus.WAITING_CUSTOMER
            ),
            slaThreshold
        );

        for (SupportTicket ticket : waitingTickets) {
            if (hasAlreadySentSlaBreach(ticket)) {
                continue;
            }

            auditService.recordEvent(
                AuditEventType.SLA_BREACH,
                ticket.getId(),
                Utils.getLoggedInUserId(),
                String.format("SLA breach: Waiting customer for %d hours", slaHours),
                null
            );

            notificationService.createNotification(
                ticket.getCustomer().getId(),
                NotificationType.SLA_REMINDER,
                "Action Required: " + ticket.getFormattedTicketNo(),
                "Your ticket requires your response. Please provide requested information.",
                ticket.getId()
            );

            if (ticket.getAssignedAgent() != null) {
                notificationService.createNotification(
                    ticket.getAssignedAgent().getId(),
                    NotificationType.SLA_REMINDER,
                    "Customer SLA Breach: " + ticket.getFormattedTicketNo(),
                    "Customer has not responded within SLA timeframe.",
                    ticket.getId()
                );
            }

            log.info("SLA breach triggered for ticket: {}", ticket.getTicketNo());
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void checkAgentSlaBreach() {
        int slaHours = PolicyConfigKey.getIntValue(
            policyConfigRepository.findByConfigKeyAndActiveTrue(PolicyConfigKey.SLA_AGENT_RESPONSE_HOURS)
                                  .map(PolicyConfig::getConfigValue)
                                  .orElseThrow(DataNotFoundException::new),
            24
        );

        Instant slaThreshold = Instant.now().minusSeconds(slaHours * 3600L);

        long breachCount = supportTicketRepository.countSlaBreaches(slaThreshold);

        if (breachCount > 0) {
            log.warn("SLA breaches detected: {} tickets", breachCount);

            List<SupportTicket> breachedTickets = supportTicketRepository.findByStatusInAndLastActivityAtBefore(
                EnumSet.of(
                    TicketStatus.ASSIGNED,
                    TicketStatus.IN_PROGRESS,
                    TicketStatus.ESCALATED
                ),
                slaThreshold
            );

            for (SupportTicket ticket : breachedTickets) {
                if (ticket.getAssignedAgent() != null) {
                    notificationService.createNotification(
                        ticket.getAssignedAgent().getId(),
                        NotificationType.SLA_REMINDER,
                        "SLA Reminder: " + ticket.getFormattedTicketNo(),
                        "Ticket requires attention - SLA approaching.",
                        ticket.getId()
                    );
                }
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkInactivityAutoClose() {
        int warningDays = PolicyConfigKey.getIntValue(
            policyConfigRepository.findByConfigKeyAndActiveTrue(PolicyConfigKey.AUTO_CLOSE_WARNING_DAYS)
                                  .map(PolicyConfig::getConfigValue)
                                  .orElseThrow(DataNotFoundException::new),
            3
        );

        int finalDays = PolicyConfigKey.getIntValue(
            policyConfigRepository.findByConfigKeyAndActiveTrue(PolicyConfigKey.AUTO_CLOSE_FINAL_DAYS)
                                  .map(PolicyConfig::getConfigValue)
                                  .orElseThrow(DataNotFoundException::new),
            7
        );

        Instant warningThreshold = Instant.now().minusSeconds(warningDays * 86400L);
        Instant finalThreshold = Instant.now().minusSeconds(finalDays * 86400L);

        List<SupportTicket> resolvedStale = supportTicketRepository.findByStatusInAndLastActivityAtBefore(
            EnumSet.of(
                TicketStatus.RESOLVED
            ),
            warningThreshold
        );

        for (SupportTicket ticket : resolvedStale) {
            if (ticket.getLastActivityAt().isBefore(finalThreshold)) {
                ticket.setStatus(TicketStatus.CLOSED);
                ticket.setClosedAt(Instant.now());
                supportTicketRepository.save(ticket);

                auditService.recordEvent(
                    AuditEventType.AUTO_CLOSE_TRIGGERED,
                    ticket.getId(),
                    Utils.getLoggedInUserId(),
                    "Auto-closed after inactivity",
                    null
                );

                log.info("Auto-closed ticket: {}", ticket.getTicketNo());
            } else if (!isAutoClosePending(ticket)) {
                ticket.setStatus(TicketStatus.AUTO_CLOSED_PENDING);
                supportTicketRepository.save(ticket);

                notificationService.createNotification(
                    ticket.getCustomer().getId(),
                    NotificationType.AUTO_CLOSE_WARNING,
                    "Ticket Closing Soon: " + ticket.getFormattedTicketNo(),
                    "Your ticket will be automatically closed due to inactivity.",
                    ticket.getId()
                );

                auditService.recordEvent(
                    AuditEventType.AUTO_CLOSE_TRIGGERED,
                    ticket.getId(),
                    Utils.getLoggedInUserId(),
                    "Auto-close warning sent",
                    null
                );

                log.info("Auto-close pending for ticket: {}", ticket.getTicketNo());
            }
        }
    }

    private boolean hasAlreadySentSlaBreach(
        SupportTicket supportTicket
    ) {
        return auditService.getTicketAuditTrail(supportTicket.getId())
                           .stream()
                           .anyMatch(auditEvent -> AuditEventType.SLA_BREACH.equals(auditEvent.getEventType()));
    }

    private boolean isAutoClosePending(
        SupportTicket supportTicket
    ) {
        return TicketStatus.AUTO_CLOSED_PENDING.equals(supportTicket.getStatus());
    }
}
