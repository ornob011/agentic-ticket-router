package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.service.policy.PolicyValueLookupService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCloseInactivityService {

    private final SupportTicketRepository supportTicketRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PolicyValueLookupService policyValueLookupService;

    public void checkInactivityAutoClose() {
        log.info(
            "AutoCloseInactivityCheck({})",
            OperationalLogContext.PHASE_START
        );

        int warningDays = policyValueLookupService.getRequiredIntValue(
            PolicyConfigKey.AUTO_CLOSE_WARNING_DAYS
        );

        int finalDays = policyValueLookupService.getRequiredIntValue(
            PolicyConfigKey.AUTO_CLOSE_FINAL_DAYS
        );
        warningDays = Math.max(warningDays, 1);
        finalDays = Math.max(finalDays, warningDays);

        Instant now = Instant.now();
        Instant warningThreshold = now.minusSeconds(warningDays * 86400L);
        Instant finalThreshold = now.minusSeconds(finalDays * 86400L);

        List<SupportTicket> resolvedStale = supportTicketRepository.findByStatusInAndLastActivityAtBefore(
            EnumSet.of(
                TicketStatus.RESOLVED,
                TicketStatus.AUTO_CLOSED_PENDING
            ),
            warningThreshold
        );

        log.info(
            "AutoCloseInactivityCheck({}) Outcome(warningDays:{},finalDays:{},candidateCount:{})",
            OperationalLogContext.PHASE_DECISION,
            warningDays,
            finalDays,
            resolvedStale.size()
        );

        for (SupportTicket ticket : resolvedStale) {
            if (ticket.getLastActivityAt().isBefore(finalThreshold)) {
                ticket.setStatus(TicketStatus.CLOSED);
                ticket.setClosedAt(now);
                supportTicketRepository.save(ticket);

                auditService.recordEvent(
                    AuditEventType.AUTO_CLOSE_TRIGGERED,
                    ticket.getId(),
                    null,
                    "Auto-closed after inactivity",
                    null
                );

                log.info(
                    "AutoCloseInactivityCheck({}) SupportTicket(id:{},ticketNo:{},status:{}) Outcome(action:{})",
                    OperationalLogContext.PHASE_COMPLETE,
                    ticket.getId(),
                    ticket.getFormattedTicketNo(),
                    ticket.getStatus(),
                    "auto_closed"
                );
            } else if (TicketStatus.RESOLVED.equals(ticket.getStatus())) {
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
                    null,
                    "Auto-close warning sent",
                    null
                );

                log.info(
                    "AutoCloseInactivityCheck({}) SupportTicket(id:{},ticketNo:{},status:{}) Outcome(action:{})",
                    OperationalLogContext.PHASE_COMPLETE,
                    ticket.getId(),
                    ticket.getFormattedTicketNo(),
                    ticket.getStatus(),
                    "auto_close_pending"
                );
            }
        }
    }
}
