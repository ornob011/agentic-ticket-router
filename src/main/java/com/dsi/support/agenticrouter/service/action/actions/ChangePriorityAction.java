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
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    ) {
        TicketPriority newPriority = routerResponse.getPriority();
        TicketPriority oldPriority = supportTicket.getCurrentPriority();

        if (Objects.isNull(newPriority)) {
            throw new IllegalStateException("Priority is required");
        }

        supportTicket.setCurrentPriority(newPriority);
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

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
    }
}
