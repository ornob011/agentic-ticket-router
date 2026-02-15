package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.event.TicketCreatedEvent;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerReplyLifecycleService {

    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final AutonomousProgressService autonomousProgressService;

    public void handleCustomerReply(
        SupportTicket supportTicket,
        AppUser customer,
        Object eventSource
    ) {
        TicketStatus ticketStatus = supportTicket.getStatus();

        if (ticketStatus == TicketStatus.WAITING_CUSTOMER) {
            handleWaitingCustomerReply(
                supportTicket,
                customer,
                eventSource
            );
            return;
        }

        if (ticketStatus == TicketStatus.CLOSED
            || ticketStatus == TicketStatus.AUTO_CLOSED_PENDING
            || ticketStatus == TicketStatus.RESOLVED
            || ticketStatus == TicketStatus.TRIAGING
            || ticketStatus == TicketStatus.RECEIVED) {
            reopenTicket(
                supportTicket,
                customer,
                eventSource
            );
            return;
        }

        if (ticketStatus == TicketStatus.IN_PROGRESS) {
            auditService.recordEvent(
                AuditEventType.MESSAGE_POSTED,
                supportTicket.getId(),
                customer.getId(),
                "Customer replied to IN_PROGRESS ticket - triggering re-triage",
                null
            );
            publishTicketCreatedEvent(
                supportTicket,
                eventSource
            );
            return;
        }

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            customer.getId(),
            "Customer added reply",
            null
        );
    }

    private void handleWaitingCustomerReply(
        SupportTicket supportTicket,
        AppUser customer,
        Object eventSource
    ) {
        if (autonomousProgressService.shouldContinueAutonomous(supportTicket)) {
            auditService.recordEvent(
                AuditEventType.MESSAGE_POSTED,
                supportTicket.getId(),
                customer.getId(),
                "Customer replied - re-triggering autonomous analysis",
                null
            );
            publishTicketCreatedEvent(
                supportTicket,
                eventSource
            );
            return;
        }

        auditService.recordEvent(
            AuditEventType.POLICY_GATE_TRIGGERED,
            supportTicket.getId(),
            null,
            "Autonomous limits reached - routing to human",
            null
        );
    }

    private void reopenTicket(
        SupportTicket supportTicket,
        AppUser customer,
        Object eventSource
    ) {
        supportTicket.setStatus(TicketStatus.RECEIVED);
        supportTicket.setRequiresHumanReview(false);
        supportTicket.incrementReopenCount();

        auditService.recordEvent(
            AuditEventType.TICKET_REOPENED,
            supportTicket.getId(),
            customer.getId(),
            "Ticket reopened by customer reply",
            null
        );

        publishTicketCreatedEvent(
            supportTicket,
            eventSource
        );
    }

    private void publishTicketCreatedEvent(
        SupportTicket supportTicket,
        Object eventSource
    ) {
        eventPublisher.publishEvent(
            new TicketCreatedEvent(
                eventSource,
                supportTicket.getId()
            )
        );
    }
}
