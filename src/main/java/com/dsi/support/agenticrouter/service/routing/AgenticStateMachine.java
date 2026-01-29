package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.MessageKind;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// TODO: Fix the class

/**
 * Executes agentic actions based on routing decisions.
 * This is the "action layer" that translates routing intent into system
 * changes.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AgenticStateMachine {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final EscalationRepository escalationRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /**
     * Execute action based on routing decision.
     *
     * @param ticket  Ticket to act on
     * @param routing Routing decision
     */
    public void executeAction(SupportTicket ticket, TicketRouting routing) {
        log.info("Executing action {} for ticket {}",
            routing.getNextAction(), ticket.getFormattedTicketNo());

        switch (routing.getNextAction()) {
            case ASSIGN_QUEUE -> assignQueue(ticket, routing);
            case ASK_CLARIFYING -> askClarifyingQuestion(ticket, routing);
            case AUTO_REPLY -> sendAutoReply(ticket, routing);
            case ESCALATE -> escalateTicket(ticket, routing);
            case HUMAN_REVIEW -> routeToHumanReview(ticket, routing);
            default -> log.warn("Unknown next action: {}", routing.getNextAction());
        }
    }

    /**
     * Assign ticket to queue and transition to ASSIGNED status.
     */
    private void assignQueue(SupportTicket ticket, TicketRouting routing) {
        log.info("Assigning ticket {} to queue {}",
            ticket.getFormattedTicketNo(), routing.getQueue());

        ticket.setAssignedQueue(routing.getQueue());
        ticket.setStatus(TicketStatus.ASSIGNED);

        if (ticket.getFirstAssignedAt() == null) {
            ticket.setFirstAssignedAt(Instant.now());
        }

        supportTicketRepository.save(ticket);

        auditService.recordEvent(
            AuditEventType.QUEUE_ASSIGNED,
            ticket.getId(),
            Utils.getLoggedInUserId(),
            String.format("Ticket assigned to queue: %s (confidence: %s)",
                routing.getQueue(), routing.getConfidence()),
            null);

        // Notify agents in queue (simplified - would query agents by queue in real
        // impl)
        log.debug("Would notify agents in queue: {}", routing.getQueue());
    }

    /**
     * Post clarifying question and wait for customer response.
     */
    private void askClarifyingQuestion(SupportTicket ticket, TicketRouting routing) {
        log.info("Posting clarifying question for ticket {}", ticket.getFormattedTicketNo());

        if (routing.getClarifyingQuestion() == null || routing.getClarifyingQuestion().isBlank()) {
            log.warn("No clarifying question provided, falling back to HUMAN_REVIEW");
            routeToHumanReview(ticket, routing);
            return;
        }

        // Post system message with question
        TicketMessage message = TicketMessage.builder()
                                             .ticket(ticket)
                                             .author(null) // System message
                                             .messageKind(MessageKind.CLARIFYING_QUESTION)
                                             .content(routing.getClarifyingQuestion())
                                             .visibleToCustomer(true)
                                             .build();

        ticketMessageRepository.save(message);

        // Transition to waiting for customer
        ticket.setStatus(TicketStatus.WAITING_CUSTOMER);
        supportTicketRepository.save(ticket);

        // Notify customer
        notificationService.createNotification(
            ticket.getCustomer().getId(),
            NotificationType.NEW_MESSAGE,
            "Clarification Needed: " + ticket.getFormattedTicketNo(),
            "We need more information to help with your ticket.",
            ticket.getId());

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            ticket.getId(),
            Utils.getLoggedInUserId(),
            "System posted clarifying question",
            null);
    }

    /**
     * Send automated reply and resolve ticket.
     */
    private void sendAutoReply(SupportTicket ticket, TicketRouting routing) {
        log.info("Sending auto-reply for ticket {}", ticket.getFormattedTicketNo());

        if (routing.getDraftReply() == null || routing.getDraftReply().isBlank()) {
            log.warn("No draft reply provided, falling back to queue assignment");
            assignQueue(ticket, routing);
            return;
        }

        // Post auto-reply message
        TicketMessage message = TicketMessage.builder()
                                             .ticket(ticket)
                                             .author(null) // System message
                                             .messageKind(MessageKind.AUTO_REPLY)
                                             .content(routing.getDraftReply())
                                             .visibleToCustomer(true)
                                             .build();

        ticketMessageRepository.save(message);

        // Mark as resolved
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(Instant.now());
        supportTicketRepository.save(ticket);

        // Notify customer
        notificationService.createNotification(
            ticket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            "Ticket Resolved: " + ticket.getFormattedTicketNo(),
            "Your ticket has been automatically resolved. If you need further assistance, please reply.",
            ticket.getId());

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            ticket.getId(),
            Utils.getLoggedInUserId(),
            "System sent auto-reply and resolved ticket",
            null);
    }

    /**
     * Escalate ticket to supervisor/specialized team.
     */
    private void escalateTicket(SupportTicket ticket, TicketRouting routing) {
        log.warn("Escalating ticket {}", ticket.getFormattedTicketNo());

        // Create escalation record
        String reason = String.format("Auto-escalated: category=%s, queue=%s, tags=%s",
            routing.getCategory(),
            routing.getQueue(),
            routing.getRationaleTags());

        Escalation escalation = Escalation.builder()
                                          .ticket(ticket)
                                          .reason(reason)
                                          .resolved(false)
                                          .build();

        escalationRepository.save(escalation);

        // Update ticket
        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setEscalated(true);
        ticket.setAssignedQueue(routing.getQueue());
        supportTicketRepository.save(ticket);

        // Notify supervisors (simplified)
        auditService.recordEvent(
            AuditEventType.ESCALATION_CREATED,
            ticket.getId(),
            null,
            "Ticket escalated: " + reason,
            null);

        log.debug("Would notify supervisors about escalation");
    }

    /**
     * Route to human review queue (low confidence or critical).
     */
    private void routeToHumanReview(SupportTicket ticket, TicketRouting routing) {
        log.info("Routing ticket {} to human review", ticket.getFormattedTicketNo());

        // Keep in TRIAGING status - requires supervisor review
        ticket.setStatus(TicketStatus.TRIAGING);
        ticket.setAssignedQueue(routing.getQueue());
        supportTicketRepository.save(ticket);

        auditService.recordEvent(
            AuditEventType.POLICY_GATE_TRIGGERED,
            ticket.getId(),
            Utils.getLoggedInUserId(),
            String.format("Routed to human review: confidence=%s, category=%s",
                routing.getConfidence(), routing.getCategory()),
            null);

        log.debug("Would notify supervisors about ticket needing review");
    }
}
