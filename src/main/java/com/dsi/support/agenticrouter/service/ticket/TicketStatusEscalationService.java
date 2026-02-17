package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.util.BindValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

@Service
@RequiredArgsConstructor
public class TicketStatusEscalationService {

    private final EscalationRepository escalationRepository;

    public void validateEscalationResolvedBeforeStatusChange(
        SupportTicket supportTicket
    ) throws BindException {
        if (supportTicket.getStatus() != TicketStatus.ESCALATED) {
            return;
        }

        boolean hasOpenEscalation = escalationRepository.findByTicketId(supportTicket.getId())
                                                        .map(existingEscalation -> !existingEscalation.isResolved())
                                                        .orElse(false);
        if (hasOpenEscalation) {
            throw BindValidation.fieldError(
                "ticketStatusRequest",
                "newStatus",
                "Please resolve the escalation before changing ticket status."
            );
        }
    }

    public void synchronizeEscalationState(
        SupportTicket supportTicket,
        TicketStatus targetStatus,
        AppUser actor,
        String normalizedBusinessDriver,
        String normalizedReason
    ) {
        if (targetStatus != TicketStatus.ESCALATED) {
            supportTicket.setEscalated(false);
            return;
        }

        String escalationReason = String.format(
            "Manual escalation by %s. Triggered by: %s. Reason: %s.",
            actor.getRole(),
            normalizedBusinessDriver,
            normalizedReason
        );

        escalationRepository.findByTicketId(supportTicket.getId())
                            .ifPresentOrElse(
                                escalation -> escalation.reopen(escalationReason),
                                () -> escalationRepository.save(
                                    Escalation.builder()
                                              .ticket(supportTicket)
                                              .reason(escalationReason)
                                              .resolved(false)
                                              .build()
                                )
                            );

        supportTicket.setEscalated(true);
    }
}
