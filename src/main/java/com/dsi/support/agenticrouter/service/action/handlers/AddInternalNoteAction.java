package com.dsi.support.agenticrouter.service.action.handlers;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddInternalNoteAction implements TicketAction {

    private final TicketMessageRepository messageRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.ADD_INTERNAL_NOTE.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        String internalNote = routerResponse.getInternalNote();

        log.info(
            "AddInternalNoteAction({}) SupportTicket(id:{},status:{}) RouterResponse(noteLength:{})",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus(),
            StringUtils.length(internalNote)
        );

        if (StringUtils.isBlank(internalNote)) {
            throw new NullPointerException(
                "Internal note content cannot be null or empty"
            );
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .messageKind(MessageKind.INTERNAL_NOTE)
                                                   .content(internalNote)
                                                   .visibleToCustomer(false)
                                                   .build();
        messageRepository.save(ticketMessage);

        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        log.info(
            "AddInternalNoteAction({}) SupportTicket(id:{},status:{}) Outcome(messageKind:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            MessageKind.INTERNAL_NOTE
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "System added internal note",
            null
        );

        log.info(
            "AddInternalNoteAction({}) SupportTicket(id:{},status:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus()
        );
    }
}
