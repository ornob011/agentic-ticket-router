package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class HumanReviewAction implements TicketAction {

    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.HUMAN_REVIEW.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        log.info(
            "HumanReviewAction({}) SupportTicket(id:{},status:{}) RouterResponse(queue:{},confidence:{},category:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routerResponse.getQueue(),
            routerResponse.getConfidence(),
            routerResponse.getCategory()
        );

        supportTicket.setStatus(TicketStatus.TRIAGING);
        supportTicket.setAssignedQueue(routerResponse.getQueue());
        supportTicket.setRequiresHumanReview(true);

        supportTicketRepository.save(supportTicket);

        log.info(
            "HumanReviewAction({}) SupportTicket(id:{},status:{},queue:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue()
        );

        auditService.recordEvent(
            AuditEventType.POLICY_GATE_TRIGGERED,
            supportTicket.getId(),
            null,
            String.format(
                "Routed to human review: confidence=%s, category=%s",
                routerResponse.getConfidence(),
                routerResponse.getCategory()
            ),
            null
        );

        log.info(
            "HumanReviewAction({}) SupportTicket(id:{},status:{},queue:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue()
        );
    }
}
