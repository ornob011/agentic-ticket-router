package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.event.TicketCreatedEvent;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.memory.MemoryContextService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketCreationWorkflowService {

    private final TicketMessageRepository ticketMessageRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final MemoryContextService memoryContextService;

    public void applyPostCreateWorkflows(
        SupportTicket supportTicket,
        AppUser customer,
        CreateTicketDto createTicketDto,
        Object eventSource
    ) {
        TicketMessage initialTicketMessage = TicketMessage.builder()
                                                          .ticket(supportTicket)
                                                          .author(customer)
                                                          .messageKind(MessageKind.CUSTOMER_MESSAGE)
                                                          .content(createTicketDto.getContent())
                                                          .visibleToCustomer(true)
                                                          .build();

        ticketMessageRepository.save(initialTicketMessage);
        memoryContextService.appendCustomerMessage(
            supportTicket,
            createTicketDto.getContent()
        );

        notificationService.createNotification(
            customer.getId(),
            NotificationType.TICKET_ACK,
            String.format("Ticket Created: %s", supportTicket.getFormattedTicketNo()),
            "Your support ticket has been received and will be reviewed shortly.",
            supportTicket.getId()
        );

        auditService.recordEvent(
            AuditEventType.TICKET_CREATED,
            supportTicket.getId(),
            customer.getId(),
            String.format("Ticket created: %s", supportTicket.getSubject()),
            null
        );

        eventPublisher.publishEvent(
            new TicketCreatedEvent(
                eventSource,
                supportTicket.getId()
            )
        );

        log.info(
            "TicketCreateWorkflow({}) SupportTicket(id:{},ticketNo:{},status:{}) Outcome(event:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getFormattedTicketNo(),
            supportTicket.getStatus(),
            TicketCreatedEvent.class.getSimpleName()
        );
    }
}
