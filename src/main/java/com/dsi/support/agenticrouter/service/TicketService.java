package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.entity.*;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.event.TicketCreatedEvent;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.*;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private static final String BUSINESS_DRIVER_MANUAL_STATUS_CHANGE = "MANUAL_STATUS_CHANGE";
    private static final String BUSINESS_DRIVER_UNSPECIFIED = "UNSPECIFIED";
    private static final String BUSINESS_DRIVER_AGENT_REPLY = "AGENT_REPLY";

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
    }

    public void addCustomerReply(
        Long ticketId,
        String content,
        Long customerId
    ) {
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
    }

    private interface ReplyHandler {
        void accept(
            SupportTicket supportTicket,
            AppUser customer
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
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(agent, "agent");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        if (!StringUtils.hasText(content)) {
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

        String normalizedBusinessDriver = Optional.ofNullable(businessDriver)
                                                  .map(String::trim)
                                                  .filter(StringUtils::hasText)
                                                  .orElse(BUSINESS_DRIVER_UNSPECIFIED);

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            agent.getId(),
            String.format(
                "Agent reply posted | driver=%s",
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
    }

    public void changeTicketStatus(
        Long ticketId,
        TicketStatus targetStatus,
        String reason
    ) {
        changeTicketStatus(ticketId,
            targetStatus,
            BUSINESS_DRIVER_MANUAL_STATUS_CHANGE,
            reason);
    }

    public void changeTicketStatus(
        Long ticketId,
        TicketStatus targetStatus,
        String businessDriver,
        String reason
    ) {
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
        if (previousStatus == targetStatus) return;

        Instant statusChangeTimestamp = Instant.now();

        supportTicket.setStatus(targetStatus);
        supportTicket.updateLastActivity();

        STATUS_SIDE_EFFECTS.getOrDefault(targetStatus, NO_STATUS_SIDE_EFFECT)
                           .accept(supportTicket, statusChangeTimestamp);

        String normalizedBusinessDriver = Optional.ofNullable(businessDriver)
                                                  .map(String::trim)
                                                  .filter(StringUtils::hasText)
                                                  .orElse(BUSINESS_DRIVER_UNSPECIFIED);

        String normalizedReason = Optional.ofNullable(reason)
                                          .map(String::trim)
                                          .filter(StringUtils::hasText)
                                          .orElse("-");

        auditService.recordEvent(
            AuditEventType.TICKET_STATUS_CHANGED,
            supportTicket.getId(),
            actor.getId(),
            String.format(
                "Status %s -> %s | driver=%s | reason=%s",
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

        supportTicketRepository.save(supportTicket);
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
    public Page<SupportTicket> listQueueTickets(
        TicketQueue ticketQueue,
        TicketStatus ticketStatus,
        Pageable pageable
    ) {
        return supportTicketRepository.findQueueTickets(
            ticketQueue,
            ticketStatus,
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
    }

    public void resolveEscalation(
        Long escalationId,
        String resolutionNotes
    ) {
        Objects.requireNonNull(escalationId, "escalationId");
        Objects.requireNonNull(resolutionNotes, "resolutionNotes");

        Escalation escalation = escalationRepository.findById(escalationId)
                                                    .orElseThrow(
                                                        DataNotFoundException.supplier(
                                                            Escalation.class,
                                                            escalationId
                                                        )
                                                    );

        if (escalation.isResolved()) {
            throw new IllegalStateException("Escalation already resolved: " + escalationId);
        }

        AppUser resolver = appUserRepository.findById(Utils.getLoggedInUserId())
                                            .orElseThrow(
                                                DataNotFoundException.supplier(
                                                    AppUser.class,
                                                    Utils.getLoggedInUserId()
                                                )
                                            );
        escalation.markResolved(
            resolver,
            resolutionNotes
        );

        escalationRepository.save(escalation);

        auditService.recordEvent(
            AuditEventType.ESCALATION_RESOLVED,
            escalation.getTicket().getId(),
            Utils.getLoggedInUserId(),
            String.format("Escalation resolved: %s", resolutionNotes),
            null
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
    public List<TicketRouting> getTicketRoutingHistory(Long ticketId) {
        return ticketRoutingRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    public void assignAgent(
        Long ticketId,
        Long agentId
    ) {
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

        if (!agent.getRole().equals(UserRole.AGENT) &&
            !agent.getRole().equals(UserRole.SUPERVISOR) &&
            !agent.getRole().equals(UserRole.ADMIN)) {
            throw new IllegalArgumentException("User must be an agent, supervisor, or admin");
        }

        AppUser previousAgent = supportTicket.getAssignedAgent();
        supportTicket.setAssignedAgent(agent);
        supportTicket.updateLastActivity();

        if (supportTicket.getStatus() == TicketStatus.ASSIGNED) {
            supportTicket.setStatus(TicketStatus.IN_PROGRESS);
        }

        supportTicketRepository.save(supportTicket);

        String description = previousAgent != null
            ? String.format("Agent reassigned: %s -> %s", previousAgent.getFullName(), agent.getFullName())
            : String.format("Your ticket has been assigned to %s by your support team", agent.getFullName());

        auditService.recordEvent(
            AuditEventType.AGENT_ASSIGNED,
            ticketId,
            agentId,
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
    }

    public void releaseAgent(Long ticketId) {
        Objects.requireNonNull(ticketId, "ticketId");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        AppUser previousAgent = supportTicket.getAssignedAgent();
        if (previousAgent == null) {
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
    }
}
