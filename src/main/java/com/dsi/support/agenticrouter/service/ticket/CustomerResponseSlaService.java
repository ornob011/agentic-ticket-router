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
public class CustomerResponseSlaService {

    private final SupportTicketRepository supportTicketRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PolicyValueLookupService policyValueLookupService;

    public void checkCustomerResponseTimeout() {
        log.info(
            "SlaCustomerTimeoutCheck({})",
            OperationalLogContext.PHASE_START
        );

        int slaHours = policyValueLookupService.getRequiredIntValue(
            PolicyConfigKey.SLA_CUSTOMER_RESPONSE_HOURS
        );
        slaHours = Math.max(slaHours, 1);

        Instant slaThreshold = Instant.now().minusSeconds(slaHours * 3600L);

        List<SupportTicket> waitingTickets = supportTicketRepository.findByStatusInAndLastActivityAtBefore(
            EnumSet.of(
                TicketStatus.WAITING_CUSTOMER
            ),
            slaThreshold
        );

        log.info(
            "SlaCustomerTimeoutCheck({}) Outcome(slaHours:{},candidateCount:{})",
            OperationalLogContext.PHASE_DECISION,
            slaHours,
            waitingTickets.size()
        );

        for (SupportTicket ticket : waitingTickets) {
            if (hasAlreadySentSlaBreach(ticket)) {
                continue;
            }

            auditService.recordEvent(
                AuditEventType.SLA_BREACH,
                ticket.getId(),
                null,
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

            log.info(
                "SlaCustomerTimeoutCheck({}) SupportTicket(id:{},ticketNo:{},status:{}) Outcome(action:{})",
                OperationalLogContext.PHASE_COMPLETE,
                ticket.getId(),
                ticket.getFormattedTicketNo(),
                ticket.getStatus(),
                "breach_notification_sent"
            );
        }
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
