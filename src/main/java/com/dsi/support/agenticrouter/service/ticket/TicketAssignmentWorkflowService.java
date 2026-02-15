package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketAssignmentWorkflowService {

    private final TicketWorkflowUpdateService ticketWorkflowUpdateService;

    public void applyAssignment(
        SupportTicket supportTicket,
        AppUser decisionActor,
        AppUser assignedAgent
    ) {
        supportTicket.setAssignedAgent(assignedAgent);

        ticketWorkflowUpdateService.completeHumanReviewIfSupervisorDecision(
            supportTicket,
            decisionActor
        );

        supportTicket.updateLastActivity();

        if (supportTicket.getStatus() == TicketStatus.ASSIGNED
            || supportTicket.getStatus() == TicketStatus.TRIAGING) {
            supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        }
    }

    public void applyRelease(
        SupportTicket supportTicket
    ) {
        supportTicket.setAssignedAgent(null);

        if (shouldTransitionToAssignedOnRelease(supportTicket.getStatus())) {
            supportTicket.setStatus(TicketStatus.ASSIGNED);
        }

        supportTicket.updateLastActivity();
    }

    private boolean shouldTransitionToAssignedOnRelease(
        TicketStatus currentStatus
    ) {
        return currentStatus == TicketStatus.TRIAGING
               || currentStatus == TicketStatus.IN_PROGRESS
               || currentStatus == TicketStatus.WAITING_CUSTOMER;
    }
}
