package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChangePriorityAction implements TicketAction {

    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;
    private final TicketMessageRepository messageRepository;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.CHANGE_PRIORITY.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        TicketPriority newPriority = routerResponse.getPriority();
        TicketPriority oldPriority = supportTicket.getCurrentPriority();

        log.info(
            "ChangePriorityAction({}) SupportTicket(id:{},status:{},priority:{}) RouterResponse(priority:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            oldPriority,
            newPriority
        );

        if (Objects.isNull(newPriority)) {
            throw BindValidation.fieldError(
                "routerResponse",
                "priority",
                "Priority is required"
            );
        }

        supportTicket.setCurrentPriority(newPriority);
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        log.info(
            "ChangePriorityAction({}) SupportTicket(id:{},status:{},priority:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getCurrentPriority()
        );

        if (!newPriority.equals(oldPriority)) {
            String messageContent = String.format(
                "Priority auto-adjusted from %s to %s",
                oldPriority,
                newPriority
            );

            TicketMessage ticketMessage = TicketMessage.builder()
                                                       .ticket(supportTicket)
                                                       .messageKind(MessageKind.SYSTEM_MESSAGE)
                                                       .content(messageContent)
                                                       .visibleToCustomer(false)
                                                       .build();
            messageRepository.save(ticketMessage);

            auditService.recordEvent(
                AuditEventType.PRIORITY_CHANGED,
                supportTicket.getId(),
                null,
                messageContent,
                null
            );
        }

        log.info(
            "ChangePriorityAction({}) SupportTicket(id:{},status:{},priority:{}) Outcome(priorityChanged:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getCurrentPriority(),
            !newPriority.equals(oldPriority)
        );
    }

}
