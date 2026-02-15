package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.event.CategoryDetectionEvent;
import com.dsi.support.agenticrouter.event.TicketCreatedEvent;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketReplyCommandService {

    private static final String BUSINESS_DRIVER_UNSPECIFIED = "System";
    private static final String BUSINESS_DRIVER_AGENT_REPLY = "Agent Reply";

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AppUserRepository appUserRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AutonomousProgressService autonomousProgressService;
    private final TicketAccessPolicyService ticketAccessPolicyService;

    public void addCustomerReply(
        Long ticketId,
        String content,
        Long customerId
    ) {
        log.info(
            "CustomerReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(contentLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            customerId,
            StringUtils.length(StringUtils.trimToNull(content))
        );

        AppUser customer = appUserRepository.findById(customerId)
                                            .orElseThrow(
                                                DataNotFoundException.supplier(
                                                    AppUser.class,
                                                    customerId
                                                )
                                            );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        if (!ticketAccessPolicyService.canReply(
            supportTicket,
            customer
        )) {
            throw new IllegalStateException("Actor cannot reply to this ticket");
        }

        if (Objects.nonNull(supportTicket.getStatus())
            && supportTicket.getStatus().isClosedForReplies()
            && Objects.nonNull(supportTicket.getCurrentCategory())
        ) {
            log.info(
                "CustomerReply({}) SupportTicket(id:{},status:{},queue:{},priority:{}) Outcome(reason:{},currentCategory:{})",
                OperationalLogContext.PHASE_DECISION,
                supportTicket.getId(),
                supportTicket.getStatus(),
                supportTicket.getAssignedQueue(),
                supportTicket.getCurrentPriority(),
                "closed_for_reply_with_category_detection",
                supportTicket.getCurrentCategory()
            );

            TicketMessage customerMessage = TicketMessage.builder()
                                                         .ticket(supportTicket)
                                                         .content(content)
                                                         .messageKind(MessageKind.CUSTOMER_MESSAGE)
                                                         .author(supportTicket.getCustomer())
                                                         .visibleToCustomer(true)
                                                         .build();

            ticketMessageRepository.save(customerMessage);

            eventPublisher.publishEvent(
                new CategoryDetectionEvent(
                    this,
                    ticketId,
                    content,
                    customerId
                )
            );

            log.info(
                "CustomerReply({}) SupportTicket(id:{},status:{}) Outcome(event:{})",
                OperationalLogContext.PHASE_COMPLETE,
                supportTicket.getId(),
                supportTicket.getStatus(),
                CategoryDetectionEvent.class.getSimpleName()
            );

            return;
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .author(customer)
                                                   .messageKind(MessageKind.CUSTOMER_MESSAGE)
                                                   .content(content)
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);

        supportTicket.updateLastActivity();

        replyHandlers().getOrDefault(supportTicket.getStatus(), defaultReplyHandler())
                       .accept(supportTicket, customer);

        supportTicketRepository.save(supportTicket);

        log.info(
            "CustomerReply({}) SupportTicket(id:{},status:{},queue:{},priority:{}) Outcome(handlerStatus:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            supportTicket.getCurrentPriority(),
            supportTicket.getStatus()
        );
    }

    public void addAgentReply(
        Long ticketId,
        String content
    ) {
        Long agentId = Utils.getLoggedInUserId();

        AppUser agent = appUserRepository.findById(agentId)
                                         .orElseThrow(
                                             DataNotFoundException.supplier(
                                                 AppUser.class,
                                                 agentId
                                             )
                                         );

        addAgentReply(
            ticketId,
            content,
            agent,
            BUSINESS_DRIVER_AGENT_REPLY
        );
    }

    public void addAgentReply(
        Long ticketId,
        String content,
        AppUser agent,
        String businessDriver
    ) {
        log.info(
            "AgentReply({}) SupportTicket(id:{}) Actor(id:{},role:{}) Outcome(contentLength:{},businessDriver:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            OperationalLogContext.actorId(agent),
            OperationalLogContext.actorRole(agent),
            StringUtils.length(StringUtils.trimToNull(content)),
            businessDriver
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(agent, "agent");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        if (!ticketAccessPolicyService.canReply(
            supportTicket,
            agent
        )) {
            throw new IllegalStateException("Actor cannot reply to this ticket");
        }

        if (!StringUtils.isNotBlank(content)) {
            log.warn(
                "AgentReply({}) SupportTicket(id:{}) Actor(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                ticketId,
                OperationalLogContext.actorId(agent),
                "empty_content"
            );

            return;
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .author(agent)
                                                   .messageKind(MessageKind.AGENT_MESSAGE)
                                                   .content(content.trim())
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);

        supportTicket.updateLastActivity();
        completeHumanReviewIfSupervisorDecision(
            supportTicket,
            agent
        );

        String normalizedBusinessDriver = Optional.ofNullable(businessDriver)
                                                  .map(String::trim)
                                                  .filter(StringUtils::isNotBlank)
                                                  .orElse(BUSINESS_DRIVER_UNSPECIFIED);

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            agent.getId(),
            String.format(
                "Agent replied to customer (%s).",
                normalizedBusinessDriver
            ),
            null
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.NEW_MESSAGE,
            String.format("New Reply: %s", supportTicket.getFormattedTicketNo()),
            "An agent has replied to your ticket.",
            supportTicket.getId()
        );

        supportTicketRepository.save(supportTicket);

        log.info(
            "AgentReply({}) SupportTicket(id:{},status:{},queue:{}) Actor(id:{}) Outcome(messageKind:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            agent.getId(),
            MessageKind.AGENT_MESSAGE
        );
    }

    private Map<TicketStatus, ReplyHandler> replyHandlers() {
        EnumMap<TicketStatus, ReplyHandler> ticketStatusReplyHandlerEnumMap = new EnumMap<>(TicketStatus.class);

        ticketStatusReplyHandlerEnumMap.put(
            TicketStatus.WAITING_CUSTOMER,
            (supportTicket, customer) -> {
                if (autonomousProgressService.shouldContinueAutonomous(supportTicket)) {
                    auditService.recordEvent(
                        AuditEventType.MESSAGE_POSTED,
                        supportTicket.getId(),
                        customer.getId(),
                        "Customer replied - re-triggering autonomous analysis",
                        null
                    );

                    eventPublisher.publishEvent(
                        new TicketCreatedEvent(
                            this,
                            supportTicket.getId()
                        )
                    );
                } else {
                    auditService.recordEvent(
                        AuditEventType.POLICY_GATE_TRIGGERED,
                        supportTicket.getId(),
                        null,
                        "Autonomous limits reached - routing to human",
                        null
                    );
                }
            }
        );

        ticketStatusReplyHandlerEnumMap.put(
            TicketStatus.CLOSED,
            reopenTicketHandler()
        );

        ticketStatusReplyHandlerEnumMap.put(
            TicketStatus.AUTO_CLOSED_PENDING,
            reopenTicketHandler()
        );

        ticketStatusReplyHandlerEnumMap.put(
            TicketStatus.RESOLVED,
            reopenTicketHandler()
        );

        ticketStatusReplyHandlerEnumMap.put(
            TicketStatus.TRIAGING,
            reopenTicketHandler()
        );

        ticketStatusReplyHandlerEnumMap.put(
            TicketStatus.RECEIVED,
            reopenTicketHandler()
        );

        ticketStatusReplyHandlerEnumMap.put(
            TicketStatus.IN_PROGRESS,
            (supportTicket, customer) -> {
                auditService.recordEvent(
                    AuditEventType.MESSAGE_POSTED,
                    supportTicket.getId(),
                    customer.getId(),
                    "Customer replied to IN_PROGRESS ticket - triggering re-triage",
                    null
                );

                eventPublisher.publishEvent(
                    new TicketCreatedEvent(
                        this,
                        supportTicket.getId()
                    )
                );
            }
        );

        return ticketStatusReplyHandlerEnumMap;
    }

    private ReplyHandler reopenTicketHandler() {
        return (supportTicket, customer) -> {
            supportTicket.setStatus(
                TicketStatus.RECEIVED
            );
            supportTicket.setRequiresHumanReview(false);

            supportTicket.incrementReopenCount();

            auditService.recordEvent(
                AuditEventType.TICKET_REOPENED,
                supportTicket.getId(),
                customer.getId(),
                "Ticket reopened by customer reply",
                null
            );

            eventPublisher.publishEvent(
                new TicketCreatedEvent(
                    this,
                    supportTicket.getId()
                )
            );
        };
    }

    private ReplyHandler defaultReplyHandler() {
        return (supportTicket, customer) -> auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            customer.getId(),
            "Customer added reply",
            null
        );
    }

    private void completeHumanReviewIfSupervisorDecision(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.nonNull(actor) && (actor.isSupervisor() || actor.isAdmin())) {
            supportTicket.setRequiresHumanReview(false);
        }
    }

    private interface ReplyHandler {
        void accept(
            SupportTicket supportTicket,
            AppUser customer
        );
    }
}
