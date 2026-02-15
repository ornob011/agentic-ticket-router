package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TicketWorkflowUpdateService {

    public void completeHumanReviewIfSupervisorDecision(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.nonNull(actor) && (actor.isSupervisor() || actor.isAdmin())) {
            supportTicket.setRequiresHumanReview(false);
        }
    }
}
