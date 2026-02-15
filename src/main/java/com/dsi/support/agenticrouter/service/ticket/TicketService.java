package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.*;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.event.CategoryDetectionEvent;
import com.dsi.support.agenticrouter.event.TicketCreatedEvent;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.*;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private static final String BUSINESS_DRIVER_MANUAL_STATUS_CHANGE = "Manual Status Update";
    private static final String BUSINESS_DRIVER_UNSPECIFIED = "System";
    private static final String BUSINESS_DRIVER_AGENT_REPLY = "Agent Reply";
    private static final BiConsumer<SupportTicket, Instant> NO_STATUS_SIDE_EFFECT = (supportTicket, statusChangeTimestamp) -> {
    };

    private static final Map<TicketStatus, BiConsumer<SupportTicket, Instant>> STATUS_SIDE_EFFECTS = new EnumMap<>(TicketStatus.class);

    static {
        STATUS_SIDE_EFFECTS.put(
            TicketStatus.RESOLVED,
            (supportTicket, statusChangeTimestamp) -> {
                if (supportTicket.getResolvedAt() == null) {
                    supportTicket.setResolvedAt(statusChangeTimestamp);
                }
            }
        );

        STATUS_SIDE_EFFECTS.put(
            TicketStatus.CLOSED,
            (supportTicket, statusChangeTimestamp) -> {
                if (supportTicket.getClosedAt() == null) {
                    supportTicket.setClosedAt(statusChangeTimestamp);
                }
            }
        );
    }

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AppUserRepository appUserRepository;
    private final TicketRoutingRepository ticketRoutingRepository;
    private final EscalationRepository escalationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AutonomousProgressService autonomousProgressService;
    private final TicketAccessPolicyService ticketAccessPolicyService;

    @Transactional(readOnly = true)
    public Page<SupportTicket> listCustomerTickets(
        Long customerId,
        Pageable pageable
    ) {
        return supportTicketRepository.findByCustomerIdOrderByCreatedAtDesc(
            customerId,
            pageable
        );
    }

    public void createTicket(
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
    }

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

    private Map<TicketStatus, ReplyHandler> replyHandlers() {
        EnumMap<TicketStatus, ReplyHandler> ticketStatusReplyHandlerEnumMap = new EnumMap<>(TicketStatus.class);

        ticketStatusReplyHandlerEnumMap.put(
            TicketStatus.WAITING_CUSTOMER, (supportTicket, customer) -> {
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

    public void changeTicketStatus(
        Long ticketId,
        TicketStatus targetStatus,
        String reason
    ) throws BindException {
        changeTicketStatus(
            ticketId,
            targetStatus,
            BUSINESS_DRIVER_MANUAL_STATUS_CHANGE,
            reason
        );
    }

    public void changeTicketStatus(
        Long ticketId,
        TicketStatus targetStatus,
        String businessDriver,
        String reason
    ) throws BindException {
        log.info(
            "TicketStatusChange({}) SupportTicket(id:{}) Outcome(targetStatus:{},businessDriver:{},reasonLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            targetStatus,
            businessDriver,
            StringUtils.length(StringUtils.trimToNull(reason))
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(targetStatus, "targetStatus");

        Long actorId = Utils.getLoggedInUserId();

        AppUser actor = appUserRepository.findById(actorId)
                                         .orElseThrow(
                                             DataNotFoundException.supplier(
                                                 AppUser.class,
                                                 actorId
                                             )
                                         );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        TicketStatus previousStatus = supportTicket.getStatus();
        if (previousStatus == targetStatus) {
            log.debug(
                "TicketStatusChange({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "no_status_change"
            );

            return;
        }

        Set<TicketStatus> allowedTransitions = ticketAccessPolicyService.allowedStatusTransitions(
            supportTicket,
            actor
        );
        if (!allowedTransitions.contains(targetStatus)) {
            throw new IllegalStateException(
                String.format(
                    "Transition %s -> %s is not allowed for actor role %s",
                    previousStatus,
                    targetStatus,
                    actor.getRole()
                )
            );
        }

        if (previousStatus == TicketStatus.ESCALATED) {
            boolean hasOpenEscalation = escalationRepository.findByTicketId(supportTicket.getId())
                                                            .map(existingEscalation -> !existingEscalation.isResolved())
                                                            .orElse(false);
            if (hasOpenEscalation) {
                throwBindValidation(
                    "ticketStatusRequest",
                    "newStatus",
                    "Please resolve the escalation before changing ticket status."
                );
            }
        }

        Instant statusChangeTimestamp = Instant.now();

        supportTicket.setStatus(targetStatus);
        completeHumanReviewIfSupervisorDecision(
            supportTicket,
            actor
        );
        supportTicket.updateLastActivity();

        STATUS_SIDE_EFFECTS.getOrDefault(targetStatus, NO_STATUS_SIDE_EFFECT)
                           .accept(supportTicket, statusChangeTimestamp);

        String normalizedBusinessDriver = Optional.ofNullable(businessDriver)
                                                  .map(String::trim)
                                                  .filter(StringUtils::isNotBlank)
                                                  .orElse(BUSINESS_DRIVER_UNSPECIFIED);

        String normalizedReason = Optional.ofNullable(reason)
                                          .map(String::trim)
                                          .filter(StringUtils::isNotBlank)
                                          .orElse("No reason provided");

        if (targetStatus == TicketStatus.ESCALATED && !StringUtils.isNotBlank(reason)) {
            throwBindValidation(
                "ticketStatusRequest",
                "reason",
                "Escalation reason is required."
            );
        }

        auditService.recordEvent(
            AuditEventType.TICKET_STATUS_CHANGED,
            supportTicket.getId(),
            actor.getId(),
            String.format(
                "Status changed from %s to %s. Triggered by: %s. Reason: %s.",
                previousStatus, targetStatus, normalizedBusinessDriver, normalizedReason
            ),
            null
        );

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            String.format("Status Updated: %s", supportTicket.getFormattedTicketNo()),
            String.format("Your ticket status is now: %s", targetStatus),
            supportTicket.getId()
        );

        if (targetStatus == TicketStatus.ESCALATED) {
            String escalationReason = String.format(
                "Manual escalation by %s. Triggered by: %s. Reason: %s.",
                actor.getRole(),
                normalizedBusinessDriver,
                normalizedReason
            );

            escalationRepository.findByTicketId(supportTicket.getId())
                                .ifPresentOrElse(
                                    escalation -> escalation.reopen(escalationReason),
                                    () -> escalationRepository.save(
                                        Escalation.builder()
                                                  .ticket(supportTicket)
                                                  .reason(escalationReason)
                                                  .resolved(false)
                                                  .build()
                                    )
                                );
            supportTicket.setEscalated(true);
        } else {
            supportTicket.setEscalated(false);
        }

        supportTicketRepository.save(supportTicket);

        log.info(
            "TicketStatusChange({}) SupportTicket(id:{},fromStatus:{},toStatus:{}) Actor(id:{},role:{}) Outcome(driver:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            previousStatus,
            supportTicket.getStatus(),
            actor.getId(),
            actor.getRole(),
            normalizedBusinessDriver
        );
    }

    @Transactional(readOnly = true)
    public SupportTicket getTicket(
        Long ticketId
    ) {
        return supportTicketRepository.findById(ticketId)
                                      .orElseThrow(
                                          DataNotFoundException.supplier(
                                              SupportTicket.class,
                                              ticketId
                                          )
                                      );
    }

    @Transactional(readOnly = true)
    public SupportTicket getTicketDetail(
        Long ticketId
    ) {
        return supportTicketRepository.findTicketDetailById(ticketId)
                                      .orElseThrow(
                                          DataNotFoundException.supplier(
                                              SupportTicket.class,
                                              ticketId
                                          )
                                      );
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> listQueueTickets(
        TicketQueue ticketQueue,
        TicketStatus ticketStatus,
        Pageable pageable
    ) {
        return supportTicketRepository.findQueueTickets(
            ticketQueue,
            ticketStatus,
            TicketStatus.queueInboxDefaults(),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public List<TicketMessage> getTicketMessages(
        Long ticketId
    ) {
        return ticketMessageRepository.findByTicketIdWithAuthorOrderByCreatedAtAsc(
            ticketId
        );
    }

    public void overrideRouting(
        Long ticketId,
        TicketQueue newQueue,
        TicketPriority newPriority,
        String reason
    ) {
        log.info(
            "RoutingOverride({}) SupportTicket(id:{}) Outcome(newQueue:{},newPriority:{},reasonLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            newQueue,
            newPriority,
            StringUtils.length(StringUtils.trimToNull(reason))
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(newQueue, "newQueue");
        Objects.requireNonNull(newPriority, "newPriority");

        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                                                      .orElseThrow(
                                                          DataNotFoundException.supplier(
                                                              SupportTicket.class,
                                                              ticketId
                                                          )
                                                      );

        AppUser actor = Utils.getLoggedInUserDetails();

        if (!ticketAccessPolicyService.canOverrideRouting(actor)) {
            throw new IllegalStateException("Actor cannot override routing");
        }

        TicketRouting latestRouting = ticketRoutingRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                                                             .stream()
                                                             .findFirst()
                                                             .orElseThrow(
                                                                 () -> new IllegalStateException("No routing found for ticket: " + ticketId)
                                                             );

        Long overriddenById = Utils.getLoggedInUserId();

        AppUser overriddenBy = appUserRepository.findById(overriddenById)
                                                .orElseThrow(
                                                    DataNotFoundException.supplier(
                                                        AppUser.class,
                                                        overriddenById
                                                    )
                                                );

        latestRouting.setOverridden(true);
        latestRouting.setOverrideReason(reason);
        latestRouting.setOverriddenBy(overriddenBy);

        ticketRoutingRepository.save(latestRouting);

        ticket.setAssignedQueue(newQueue);
        ticket.setCurrentPriority(newPriority);
        completeHumanReviewIfSupervisorDecision(
            ticket,
            actor
        );
        ticket.updateLastActivity();

        supportTicketRepository.save(ticket);

        auditService.recordEvent(
            AuditEventType.ROUTING_OVERRIDDEN,
            ticketId,
            Utils.getLoggedInUserId(),
            String.format("Routing overridden: queue=%s, priority=%s, reason=%s",
                newQueue,
                newPriority,
                reason
            ),
            null
        );

        log.info(
            "RoutingOverride({}) SupportTicket(id:{},status:{},queue:{},priority:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId(),
            ticket.getStatus(),
            ticket.getAssignedQueue(),
            ticket.getCurrentPriority(),
            overriddenById
        );
    }

    public void resolveEscalation(
        Long escalationId,
        String resolutionNotes
    ) throws BindException {
        log.info(
            "EscalationResolve({}) Escalation(id:{}) Actor(id:{}) Outcome(notesLength:{})",
            OperationalLogContext.PHASE_START,
            escalationId,
            Utils.getLoggedInUserId(),
            StringUtils.length(StringUtils.trimToNull(resolutionNotes))
        );

        Objects.requireNonNull(escalationId, "escalationId");
        Objects.requireNonNull(resolutionNotes, "resolutionNotes");
        if (!StringUtils.isNotBlank(resolutionNotes)) {
            throwBindValidation(
                "resolveEscalationRequest",
                "resolutionNotes",
                "Resolution notes are required."
            );
        }

        Escalation escalation = escalationRepository.findById(escalationId)
                                                    .orElseThrow(
                                                        DataNotFoundException.supplier(
                                                            Escalation.class,
                                                            escalationId
                                                        )
                                                    );

        if (escalation.isResolved()) {
            log.warn(
                "EscalationResolve({}) Escalation(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                escalationId,
                "already_resolved"
            );

            throw new IllegalStateException("Escalation already resolved: " + escalationId);
        }

        AppUser resolver = appUserRepository.findById(Utils.getLoggedInUserId())
                                            .orElseThrow(
                                                DataNotFoundException.supplier(
                                                    AppUser.class,
                                                    Utils.getLoggedInUserId()
                                                )
                                            );
        if (!ticketAccessPolicyService.canResolveEscalation(resolver)) {
            throw new IllegalStateException("Actor cannot resolve escalation");
        }

        escalation.markResolved(
            resolver,
            resolutionNotes
        );

        escalationRepository.save(escalation);

        SupportTicket supportTicket = escalation.getTicket();
        supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        supportTicket.setEscalated(false);
        supportTicket.setRequiresHumanReview(false);
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.ESCALATION_RESOLVED,
            escalation.getTicket().getId(),
            Utils.getLoggedInUserId(),
            String.format("Escalation resolved: %s", resolutionNotes),
            null
        );

        log.info(
            "EscalationResolve({}) Escalation(id:{}) SupportTicket(id:{},status:{}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            escalation.getId(),
            escalation.getTicket().getId(),
            escalation.getTicket().getStatus(),
            resolver.getId()
        );
    }

    public List<Escalation> getPendingEscalations() {
        return escalationRepository.findPendingEscalations();
    }

    @Transactional(readOnly = true)
    public Page<Escalation> listEscalationsByResolved(
        boolean resolved,
        Pageable pageable
    ) {
        if (resolved) {
            return escalationRepository.findByResolvedTrue(pageable);
        } else {
            return escalationRepository.findByResolvedFalse(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<Escalation> listAllEscalations(Pageable pageable) {
        return escalationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Escalation getEscalationById(Long escalationId) {
        return escalationRepository.findById(escalationId)
                                   .orElseThrow(
                                       DataNotFoundException.supplier(
                                           Escalation.class,
                                           escalationId
                                       )
                                   );
    }

    @Transactional(readOnly = true)
    public ApiDtos.PagedResponse<ApiDtos.EscalationSummary> listEscalationSummaries(
        EscalationFilterStatus escalationFilterStatus,
        Pageable pageable
    ) {
        EscalationFilterStatus effectiveFilterStatus = Objects.requireNonNullElse(
            escalationFilterStatus,
            EscalationFilterStatus.ALL
        );

        Page<Escalation> escalationPage = switch (effectiveFilterStatus) {
            case RESOLVED -> escalationRepository.findByResolvedTrue(pageable);
            case PENDING -> escalationRepository.findByResolvedFalse(pageable);
            case ALL -> escalationRepository.findAll(pageable);
        };

        List<ApiDtos.EscalationSummary> content = escalationPage.map(this::toEscalationSummaryDto)
                                                                .getContent();

        return ApiDtos.PagedResponse.<ApiDtos.EscalationSummary>builder()
                                    .content(content)
                                    .page(escalationPage.getNumber())
                                    .size(escalationPage.getSize())
                                    .totalElements(escalationPage.getTotalElements())
                                    .totalPages(escalationPage.getTotalPages())
                                    .hasNext(escalationPage.hasNext())
                                    .build();
    }

    @Transactional(readOnly = true)
    public ApiDtos.EscalationDetail getEscalationDetail(
        Long escalationId
    ) {
        Escalation escalation = escalationRepository.findById(escalationId)
                                                    .orElseThrow(
                                                        DataNotFoundException.supplier(
                                                            Escalation.class,
                                                            escalationId
                                                        )
                                                    );

        return ApiDtos.EscalationDetail.builder()
                                       .id(escalation.getId())
                                       .ticketId(escalation.getTicket().getId())
                                       .formattedTicketNo(escalation.getTicket().getFormattedTicketNo())
                                       .reason(escalation.getReason())
                                       .resolved(escalation.isResolved())
                                       .resolutionNotes(escalation.getResolutionNotes())
                                       .createdAt(escalation.getCreatedAt())
                                       .resolvedAt(escalation.getResolvedAt())
                                       .assignedSupervisor(
                                           Objects.isNull(escalation.getAssignedSupervisor())
                                               ? null
                                               : escalation.getAssignedSupervisor().getFullName()
                                       )
                                       .resolvedBy(
                                           Objects.isNull(escalation.getResolvedBy())
                                               ? null
                                               : escalation.getResolvedBy().getFullName()
                                       )
                                       .build();
    }

    private ApiDtos.EscalationSummary toEscalationSummaryDto(Escalation escalation) {
        return ApiDtos.EscalationSummary.builder()
                                        .id(escalation.getId())
                                        .ticketId(escalation.getTicket().getId())
                                        .formattedTicketNo(escalation.getTicket().getFormattedTicketNo())
                                        .reason(escalation.getReason())
                                        .resolved(escalation.isResolved())
                                        .createdAt(escalation.getCreatedAt())
                                        .assignedSupervisor(
                                            Objects.isNull(escalation.getAssignedSupervisor())
                                                ? null
                                                : escalation.getAssignedSupervisor().getFullName()
                                        )
                                        .build();
    }

    private void completeHumanReviewIfSupervisorDecision(
        SupportTicket supportTicket,
        AppUser actor
    ) {
        if (Objects.nonNull(actor) && (actor.isSupervisor() || actor.isAdmin())) {
            supportTicket.setRequiresHumanReview(false);
        }
    }

    private void throwBindValidation(
        String objectName,
        String fieldName,
        String message
    ) throws BindException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
            new Object(),
            objectName
        );

        bindingResult.rejectValue(
            fieldName,
            "validation.error",
            message
        );

        throw new BindException(bindingResult);
    }

    @Transactional(readOnly = true)
    public List<TicketRouting> getTicketRoutingHistory(Long ticketId) {
        return ticketRoutingRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    public void assignSelf(
        Long ticketId
    ) {
        Objects.requireNonNull(ticketId, "ticketId");

        AppUser actor = Utils.getLoggedInUserDetails();
        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        if (!ticketAccessPolicyService.canAssignSelf(
            supportTicket,
            actor
        )) {
            throw new IllegalStateException("Actor cannot self-assign this ticket");
        }

        AppUser previousAgent = supportTicket.getAssignedAgent();
        supportTicket.setAssignedAgent(actor);
        completeHumanReviewIfSupervisorDecision(
            supportTicket,
            actor
        );
        supportTicket.updateLastActivity();

        if (supportTicket.getStatus() == TicketStatus.ASSIGNED
            || supportTicket.getStatus() == TicketStatus.TRIAGING) {
            supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        }

        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.AGENT_ASSIGNED,
            ticketId,
            actor.getId(),
            String.format(
                "Agent self-assigned: %s (previousAgentId=%s)",
                actor.getFullName(),
                Objects.nonNull(previousAgent) ? previousAgent.getId() : null
            ),
            null
        );
    }

    public void assignAgent(
        Long ticketId,
        Long agentId
    ) throws BindException {
        log.info(
            "AgentAssign({}) SupportTicket(id:{}) Actor(targetAgentId:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            agentId
        );

        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(agentId, "agentId");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        AppUser agent = appUserRepository.findById(agentId)
                                         .orElseThrow(
                                             DataNotFoundException.supplier(
                                                 AppUser.class,
                                                 agentId
                                             )
                                         );

        AppUser actor = Utils.getLoggedInUserDetails();
        if (!ticketAccessPolicyService.canAssignOthers(actor)) {
            throw new IllegalStateException("Actor cannot assign other agents");
        }

        if (!agent.getRole().equals(UserRole.AGENT) &&
            !agent.getRole().equals(UserRole.SUPERVISOR) &&
            !agent.getRole().equals(UserRole.ADMIN)) {
            throwBindValidation(
                "assignAgentRequest",
                "agentId",
                "Selected user must be an agent, supervisor, or admin."
            );
        }

        AppUser previousAgent = supportTicket.getAssignedAgent();
        supportTicket.setAssignedAgent(agent);
        completeHumanReviewIfSupervisorDecision(
            supportTicket,
            actor
        );
        supportTicket.updateLastActivity();

        if (supportTicket.getStatus() == TicketStatus.ASSIGNED
            || supportTicket.getStatus() == TicketStatus.TRIAGING) {
            supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        }

        supportTicketRepository.save(supportTicket);

        String description = previousAgent != null
            ? String.format("Agent reassigned: %s -> %s", previousAgent.getFullName(), agent.getFullName())
            : String.format("Your ticket has been assigned to %s by your support team", agent.getFullName());

        auditService.recordEvent(
            AuditEventType.AGENT_ASSIGNED,
            ticketId,
            actor.getId(),
            description,
            null
        );

        notificationService.createNotification(
            agent.getId(),
            NotificationType.ASSIGNED_TO_YOU,
            "New Assignment: " + supportTicket.getFormattedTicketNo(),
            "You have been assigned to this ticket.",
            ticketId
        );

        log.info(
            "AgentAssign({}) SupportTicket(id:{},status:{},queue:{}) Actor(agentId:{},role:{}) Outcome(previousAgentId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            agent.getId(),
            agent.getRole(),
            Objects.nonNull(previousAgent) ? previousAgent.getId() : null
        );
    }

    public void releaseAgent(Long ticketId) {
        log.info(
            "AgentRelease({}) SupportTicket(id:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            Utils.getLoggedInUserId()
        );

        Objects.requireNonNull(ticketId, "ticketId");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        AppUser actor = Utils.getLoggedInUserDetails();
        if (actor.isAgent()
            && Objects.nonNull(supportTicket.getAssignedAgent())
            && !Objects.equals(
            supportTicket.getAssignedAgent().getId(),
            actor.getId()
        )) {
            throw new IllegalStateException("Agent can only release self assignment");
        }

        AppUser previousAgent = supportTicket.getAssignedAgent();
        if (previousAgent == null) {
            log.debug(
                "AgentRelease({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "already_unassigned"
            );

            return;
        }

        supportTicket.setAssignedAgent(null);
        supportTicket.setStatus(TicketStatus.ASSIGNED);
        supportTicket.updateLastActivity();
        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.AGENT_ASSIGNED,
            ticketId,
            Utils.getLoggedInUserId(),
            String.format("Agent released: %s", previousAgent.getFullName()),
            null
        );

        log.info(
            "AgentRelease({}) SupportTicket(id:{},status:{},queue:{}) Outcome(previousAgentId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getAssignedQueue(),
            previousAgent.getId()
        );
    }

    private interface ReplyHandler {
        void accept(
            SupportTicket supportTicket,
            AppUser customer
        );
    }
}
