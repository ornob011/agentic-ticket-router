package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EscalateAction implements TicketAction {

    private final EscalationRepository escalationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.ESCALATE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        String reason = String.format(
            "Auto-escalated: category=%s, queue=%s, tags=%s",
            routerResponse.getCategory(),
            routerResponse.getQueue(),
            routerResponse.getRationaleTags()
        );

        Escalation escalation = Escalation.builder()
                                          .ticket(supportTicket)
                                          .reason(reason)
                                          .resolved(false)
                                          .build();

        escalationRepository.save(escalation);

        supportTicket.setStatus(TicketStatus.ESCALATED);
        supportTicket.setEscalated(true);
        supportTicket.setAssignedQueue(routerResponse.getQueue());

        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.ESCALATION_CREATED,
            supportTicket.getId(),
            null,
            "Ticket escalated: " + reason,
            null
        );
    }
}
