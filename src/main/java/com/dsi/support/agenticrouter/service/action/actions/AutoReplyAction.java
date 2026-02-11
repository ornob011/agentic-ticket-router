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
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoReplyAction implements TicketAction {

    private final TicketMessageRepository messageRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SupportTicketRepository supportTicketRepository;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.AUTO_REPLY.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        log.info(
            "AutoReplyAction({}) SupportTicket(id:{},status:{}) RouterResponse(confidence:{},draftReplyLength:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routerResponse.getConfidence(),
            StringUtils.length(routerResponse.getDraftReply())
        );

        if (StringUtils.isBlank(routerResponse.getDraftReply())) {
            log.warn(
                "AutoReplyAction({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "missing_draft_reply"
            );

            throw new IllegalStateException("Draft reply is required");
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .messageKind(MessageKind.AUTO_REPLY)
                                                   .content(routerResponse.getDraftReply())
                                                   .visibleToCustomer(true)
                                                   .build();
        messageRepository.save(ticketMessage);

        supportTicket.setStatus(TicketStatus.RESOLVED);
        supportTicket.setResolvedAt(Instant.now());
        supportTicketRepository.save(supportTicket);

        log.info(
            "AutoReplyAction({}) SupportTicket(id:{},status:{}) Outcome(messageKind:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            MessageKind.AUTO_REPLY
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            "Ticket Resolved: " + supportTicket.getFormattedTicketNo(),
            "Your ticket has been automatically resolved. If you need further assistance, please reply.",
            supportTicket.getId()
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "System sent auto-reply and resolved ticket",
            null
        );

        log.info(
            "AutoReplyAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus()
        );
    }
}
