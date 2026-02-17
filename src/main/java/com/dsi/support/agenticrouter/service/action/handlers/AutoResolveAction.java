package com.dsi.support.agenticrouter.service.action.handlers;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketStatus;
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

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoResolveAction implements TicketAction {

    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;
    private final TicketMessageRepository messageRepository;
    private final NotificationService notificationService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.AUTO_RESOLVE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        String solution = routerResponse.getDraftReply();

        log.info(
            "AutoResolveAction({}) SupportTicket(id:{},status:{}) RouterResponse(draftReplyLength:{},confidence:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            StringUtils.length(solution),
            routerResponse.getConfidence()
        );

        if (StringUtils.isBlank(solution)) {
            throw BindValidation.fieldError(
                "routerResponse",
                "draftReply",
                "Solution content is required"
            );
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .messageKind(com.dsi.support.agenticrouter.enums.MessageKind.AUTO_REPLY)
                                                   .content(solution)
                                                   .visibleToCustomer(true)
                                                   .build();
        messageRepository.save(ticketMessage);

        supportTicket.setStatus(TicketStatus.RESOLVED);
        supportTicket.setResolvedAt(Instant.now());
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        log.info(
            "AutoResolveAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus()
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            com.dsi.support.agenticrouter.enums.NotificationType.STATUS_CHANGE,
            "Ticket Resolved: " + supportTicket.getFormattedTicketNo(),
            "Your ticket has been automatically resolved. If you need further assistance, please reply.",
            supportTicket.getId()
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "System sent resolution and resolved ticket",
            null
        );

        log.info(
            "AutoResolveAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus()
        );
    }

}
