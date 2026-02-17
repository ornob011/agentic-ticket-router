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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentResponseSlaService {

    private final SupportTicketRepository supportTicketRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PolicyValueLookupService policyValueLookupService;

    public void checkAgentSlaBreach() {
        log.info(
            "SlaAgentTimeoutCheck({})",
            OperationalLogContext.PHASE_START
        );

        int slaHours = policyValueLookupService.getRequiredIntValue(
            PolicyConfigKey.SLA_AGENT_RESPONSE_HOURS
        );
        slaHours = Math.max(slaHours, 1);

        Instant slaThreshold = Instant.now().minusSeconds(slaHours * 3600L);

        long breachCount = supportTicketRepository.countSlaBreaches(slaThreshold);

        if (breachCount > 0) {
            log.warn(
                "SlaAgentTimeoutCheck({}) Outcome(slaHours:{},breachCount:{})",
                OperationalLogContext.PHASE_DECISION,
                slaHours,
                breachCount
            );

            List<SupportTicket> breachedTickets = supportTicketRepository.findByStatusInAndLastActivityAtBefore(
                EnumSet.of(
                    TicketStatus.ASSIGNED,
                    TicketStatus.IN_PROGRESS,
                    TicketStatus.ESCALATED
                ),
                slaThreshold
            );

            for (SupportTicket ticket : breachedTickets) {
                if (hasAlreadySentSlaBreach(ticket)) {
                    continue;
                }

                auditService.recordEvent(
                    AuditEventType.SLA_BREACH,
                    ticket.getId(),
                    null,
                    String.format("SLA breach: Agent response pending for %d hours", slaHours),
                    null
                );

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

        log.info(
            "SlaAgentTimeoutCheck({}) Outcome(slaHours:{},breachCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            slaHours,
            breachCount
        );
    }

    private boolean hasAlreadySentSlaBreach(
        SupportTicket supportTicket
    ) {
        Instant since = Objects.requireNonNullElse(
            supportTicket.getLastActivityAt(),
            Instant.EPOCH
        );

        return auditService.hasEventTypeSince(
            supportTicket.getId(),
            AuditEventType.SLA_BREACH,
            since
        );
    }
}
