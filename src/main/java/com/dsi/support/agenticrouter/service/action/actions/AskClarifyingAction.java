package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AskClarifyingAction implements TicketAction {

    private final TicketMessageRepository messageRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SupportTicketRepository supportTicketRepository;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.ASK_CLARIFYING.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        if (StringUtils.isBlank(routerResponse.getClarifyingQuestion())) {
            throw new IllegalStateException("Clarifying question is required");
        }

        TicketMessage message = TicketMessage.builder()
                                             .ticket(supportTicket)
                                             .messageKind(MessageKind.CLARIFYING_QUESTION)
                                             .content(routerResponse.getClarifyingQuestion())
                                             .visibleToCustomer(true)
                                             .build();
        messageRepository.save(message);

        supportTicket.recordClarifyingQuestion(routerResponse.getClarifyingQuestion());
        supportTicket.setStatus(TicketStatus.WAITING_CUSTOMER);
        supportTicketRepository.save(supportTicket);

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.NEW_MESSAGE,
            "Clarification Needed: " + supportTicket.getFormattedTicketNo(),
            "We need more information to help with your ticket.",
            supportTicket.getId()
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "System posted clarifying question",
            null
        );
    }
}
