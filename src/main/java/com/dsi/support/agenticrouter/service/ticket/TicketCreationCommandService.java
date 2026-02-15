package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.event.TicketCreatedEvent;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketCreationCommandService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AppUserRepository appUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SupportTicket createTicket(
        CreateTicketDto createTicketDto,
        Long customerId
    ) {
        log.info(
            "TicketCreate({}) Actor(id:{}) Outcome(subjectLength:{},contentLength:{})",
            OperationalLogContext.PHASE_START,
            customerId,
            StringUtils.length(StringUtils.trimToNull(createTicketDto.getSubject())),
            StringUtils.length(StringUtils.trimToNull(createTicketDto.getContent()))
        );

        AppUser customer = appUserRepository.findById(customerId)
                                            .orElseThrow(
                                                DataNotFoundException.supplier(
                                                    AppUser.class,
                                                    customerId
                                                )
                                            );

        SupportTicket supportTicket = SupportTicket.builder()
                                                   .customer(customer)
                                                   .subject(createTicketDto.getSubject())
                                                   .status(TicketStatus.RECEIVED)
                                                   .lastActivityAt(Instant.now())
                                                   .build();

        supportTicket = supportTicketRepository.save(supportTicket);

        log.info(
            "TicketCreate({}) SupportTicket(id:{},ticketNo:{},status:{},queue:{},priority:{}) Actor(id:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getFormattedTicketNo(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            supportTicket.getCurrentPriority(),
            customerId
        );

        TicketMessage initialTicketMessage = TicketMessage.builder()
                                                          .ticket(supportTicket)
                                                          .author(customer)
                                                          .messageKind(MessageKind.CUSTOMER_MESSAGE)
                                                          .content(createTicketDto.getContent())
                                                          .visibleToCustomer(true)
                                                          .build();

        ticketMessageRepository.save(initialTicketMessage);

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
                this,
                supportTicket.getId()
            )
        );

        log.info(
            "TicketCreate({}) SupportTicket(id:{},ticketNo:{},status:{}) Outcome(event:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getFormattedTicketNo(),
            supportTicket.getStatus(),
            TicketCreatedEvent.class.getSimpleName()
        );

        return supportTicket;
    }
}
