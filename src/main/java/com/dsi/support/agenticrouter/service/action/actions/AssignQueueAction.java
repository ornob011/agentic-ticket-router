package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignQueueAction implements TicketAction {

    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.ASSIGN_QUEUE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        supportTicket.setAssignedQueue(routerResponse.getQueue());
        supportTicket.setStatus(TicketStatus.ASSIGNED);

        if (supportTicket.getFirstAssignedAt() == null) {
            supportTicket.setFirstAssignedAt(Instant.now());
        }

        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.QUEUE_ASSIGNED,
            supportTicket.getId(),
            null,
            String.format(
                "Ticket assigned to queue: %s (confidence: %s)",
                routerResponse.getQueue(),
                routerResponse.getConfidence()
            ),
            null
        );
    }
}
