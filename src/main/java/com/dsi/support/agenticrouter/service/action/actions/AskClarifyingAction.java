package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

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
    ) throws BindException {
        log.info(
            "AskClarifyingAction({}) SupportTicket(id:{},status:{}) RouterResponse(questionLength:{},confidence:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            StringUtils.length(routerResponse.getClarifyingQuestion()),
            routerResponse.getConfidence()
        );

        if (StringUtils.isBlank(routerResponse.getClarifyingQuestion())) {
            throw BindValidation.fieldError(
                "routerResponse",
                "clarifyingQuestion",
                "Clarifying question is required"
            );
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
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        log.info(
            "AskClarifyingAction({}) SupportTicket(id:{},status:{}) Outcome(questionCount:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getQuestionCount()
        );

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

        log.info(
            "AskClarifyingAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus()
        );
    }

}
