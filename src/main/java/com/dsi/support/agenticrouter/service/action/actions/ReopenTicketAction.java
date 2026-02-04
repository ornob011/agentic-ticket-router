package com.dsi.support.agenticrouter.service.action.actions;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.service.action.TicketAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReopenTicketAction implements TicketAction {

    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    public boolean canHandle(
        NextAction actionType
    ) {
        return NextAction.REOPEN_TICKET.equals(actionType);
    }

    @Override
    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        supportTicket.setStatus(TicketStatus.RECEIVED);
        supportTicket.setResolvedAt(null);
        supportTicket.setClosedAt(null);
        supportTicket.incrementReopenCount();
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.TICKET_REOPENED,
            supportTicket.getId(),
            null,
            "Ticket reopened by system action",
            null
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            com.dsi.support.agenticrouter.enums.NotificationType.TICKET_REOPENED,
            "Ticket Reopened: " + supportTicket.getFormattedTicketNo(),
            "Your ticket has been reopened and will be processed.",
            supportTicket.getId()
        );
    }
}
