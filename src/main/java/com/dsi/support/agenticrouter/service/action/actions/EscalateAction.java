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
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
        log.info(
            "EscalateAction({}) SupportTicket(id:{},status:{}) RouterResponse(queue:{},category:{},confidence:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routerResponse.getQueue(),
            routerResponse.getCategory(),
            routerResponse.getConfidence()
        );

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

        log.warn(
            "EscalateAction({}) SupportTicket(id:{},status:{},queue:{}) Escalation(id:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            escalation.getId()
        );

        auditService.recordEvent(
            AuditEventType.ESCALATION_CREATED,
            supportTicket.getId(),
            null,
            "Ticket escalated: " + reason,
            null
        );

        log.warn(
            "EscalateAction({}) SupportTicket(id:{},status:{},queue:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue()
        );
    }
}
