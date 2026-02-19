package com.dsi.support.agenticrouter.service.agentruntime.tools;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketActionTools {

    private final TicketMessageRepository messageRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Tool(description = "Assign ticket to a queue for agent handling")
    @Transactional
    public void assignQueue(
        @ToolParam(description = "The queue code: BILLING_Q, TECH_Q, OPS_Q, SECURITY_Q, ACCOUNT_Q, GENERAL_Q") String queue
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();
        TicketQueue assignedQueue = TicketQueue.valueOf(queue);

        log.info(
            "ToolExecution(assignQueue)({}) SupportTicket(id:{}) Queue({})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            assignedQueue
        );

        ticket.setAssignedQueue(
            assignedQueue
        );

        if (ticket.getStatus() == TicketStatus.RECEIVED || ticket.getStatus() == TicketStatus.TRIAGING) {
            ticket.setStatus(
                TicketStatus.ASSIGNED
            );
        }

        persistTicketActivity(
            ticket
        );

        auditService.recordEvent(
            AuditEventType.QUEUE_ASSIGNED,
            ticket.getId(),
            null,
            "Queue assigned: " + assignedQueue.name(),
            null
        );

        log.info(
            "ToolExecution(assignQueue)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Escalate ticket to supervisor or human review workflow")
    @Transactional
    public void escalate(
        @ToolParam(description = "Escalation reason") String reason
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();

        log.info(
            "ToolExecution(escalate)({}) SupportTicket(id:{}) Reason(length:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            StringUtils.length(reason)
        );

        ticket.setStatus(
            TicketStatus.ESCALATED
        );
        ticket.setEscalated(
            true
        );
        ticket.setRequiresHumanReview(
            true
        );

        persistTicketActivity(
            ticket
        );

        auditService.recordEvent(
            AuditEventType.ESCALATION_CREATED,
            ticket.getId(),
            null,
            "Ticket escalated: " + StringUtils.defaultIfBlank(reason, "No reason provided"),
            null
        );

        notificationService.createNotification(
            ticket.getCustomer().getId(),
            NotificationType.ESCALATION,
            "Ticket Escalated: " + ticket.getFormattedTicketNo(),
            StringUtils.defaultIfBlank(reason, "Your ticket has been escalated for specialist review."),
            ticket.getId()
        );

        log.info(
            "ToolExecution(escalate)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Send an automated reply to the customer and resolve the ticket")
    @Transactional
    public void autoReply(
        @ToolParam(description = "The reply content to send to the customer") String content
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();

        log.info(
            "ToolExecution(autoReply)({}) SupportTicket(id:{}) Content(length:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            StringUtils.length(content)
        );

        TicketMessage message = buildCustomerVisibleMessage(
            ticket,
            MessageKind.AUTO_REPLY,
            content
        );

        messageRepository.save(
            message
        );

        resolveTicket(
            ticket
        );

        notificationService.createNotification(
            ticket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            "Ticket Resolved: " + ticket.getFormattedTicketNo(),
            "Your ticket has been automatically resolved.",
            ticket.getId()
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            ticket.getId(),
            null,
            "System sent auto-reply and resolved ticket",
            null
        );

        log.info(
            "ToolExecution(autoReply)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Request clarifying information from the customer")
    @Transactional
    public void askClarifying(
        @ToolParam(description = "The clarifying question to ask the customer") String question
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();

        log.info(
            "ToolExecution(askClarifying)({}) SupportTicket(id:{}) Question(length:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            StringUtils.length(question)
        );

        TicketMessage message = buildCustomerVisibleMessage(
            ticket,
            MessageKind.CLARIFYING_QUESTION,
            question
        );

        messageRepository.save(
            message
        );

        ticket.setStatus(
            TicketStatus.WAITING_CUSTOMER
        );

        persistTicketActivity(
            ticket
        );

        notificationService.createNotification(
            ticket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            "Question about your ticket: " + ticket.getFormattedTicketNo(),
            question,
            ticket.getId()
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            ticket.getId(),
            null,
            "System asked clarifying question",
            null
        );

        log.info(
            "ToolExecution(askClarifying)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Change the ticket priority level")
    @Transactional
    public void changePriority(
        @ToolParam(description = "The new priority level: LOW, MEDIUM, HIGH, or URGENT") String priority
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();
        TicketPriority newPriority = TicketPriority.valueOf(priority);

        log.info(
            "ToolExecution(changePriority)({}) SupportTicket(id:{}) OldPriority({}) NewPriority({})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            ticket.getCurrentPriority(),
            newPriority
        );

        ticket.setCurrentPriority(
            newPriority
        );

        persistTicketActivity(
            ticket
        );

        auditService.recordEvent(
            AuditEventType.PRIORITY_CHANGED,
            ticket.getId(),
            null,
            "Priority changed to " + newPriority.getDisplayName(),
            null
        );

        log.info(
            "ToolExecution(changePriority)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Add an internal note visible only to agents")
    @Transactional
    public void addInternalNote(
        @ToolParam(description = "The internal note content") String note
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();

        log.info(
            "ToolExecution(addInternalNote)({}) SupportTicket(id:{}) Note(length:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            StringUtils.length(note)
        );

        TicketMessage message = buildInternalMessage(
            ticket,
            MessageKind.INTERNAL_NOTE,
            note
        );

        messageRepository.save(
            message
        );

        persistTicketActivity(
            ticket
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            ticket.getId(),
            null,
            "Internal note added",
            null
        );

        log.info(
            "ToolExecution(addInternalNote)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Mark the ticket for human review")
    @Transactional
    public void markHumanReview(
        @ToolParam(description = "The reason for requiring human review") String reason
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();

        log.info(
            "ToolExecution(markHumanReview)({}) SupportTicket(id:{}) Reason(length:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            StringUtils.length(reason)
        );

        ticket.setStatus(
            TicketStatus.TRIAGING
        );

        ticket.setRequiresHumanReview(
            true
        );

        persistTicketActivity(
            ticket
        );

        auditService.recordEvent(
            AuditEventType.TICKET_STATUS_CHANGED,
            ticket.getId(),
            null,
            "Marked for human review: " + reason,
            null
        );

        log.info(
            "ToolExecution(markHumanReview)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Auto-resolve the ticket with a solution")
    @Transactional
    public void autoResolve(
        @ToolParam(description = "The solution content to send to the customer") String solution
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();

        log.info(
            "ToolExecution(autoResolve)({}) SupportTicket(id:{}) Solution(length:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            StringUtils.length(solution)
        );

        TicketMessage message = buildCustomerVisibleMessage(
            ticket,
            MessageKind.AUTO_REPLY,
            solution
        );

        messageRepository.save(
            message
        );

        resolveTicket(
            ticket
        );

        notificationService.createNotification(
            ticket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            "Ticket Resolved: " + ticket.getFormattedTicketNo(),
            "Your ticket has been resolved.",
            ticket.getId()
        );

        auditService.recordEvent(
            AuditEventType.TICKET_STATUS_CHANGED,
            ticket.getId(),
            null,
            "Ticket auto-resolved",
            null
        );

        log.info(
            "ToolExecution(autoResolve)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Reopen a previously resolved or closed ticket")
    @Transactional
    public void reopenTicket(
        @ToolParam(description = "Reason for reopening") String reason
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();

        log.info(
            "ToolExecution(reopenTicket)({}) SupportTicket(id:{}) Reason(length:{})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            StringUtils.length(reason)
        );

        ticket.setStatus(
            TicketStatus.IN_PROGRESS
        );
        ticket.setResolvedAt(
            null
        );
        ticket.setClosedAt(
            null
        );
        ticket.setEscalated(
            false
        );
        ticket.setRequiresHumanReview(
            false
        );

        persistTicketActivity(
            ticket
        );

        auditService.recordEvent(
            AuditEventType.TICKET_REOPENED,
            ticket.getId(),
            null,
            "Ticket reopened: " + StringUtils.defaultIfBlank(reason, "No reason provided"),
            null
        );

        log.info(
            "ToolExecution(reopenTicket)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    @Tool(description = "Send a notification to relevant parties")
    @Transactional
    public void triggerNotification(
        @ToolParam(description = "Notification type: STATUS_CHANGE, NEW_MESSAGE, or ESCALATION") String notificationType,
        @ToolParam(description = "Notification title") String title,
        @ToolParam(description = "Notification body") String body
    ) {
        SupportTicket ticket = ToolExecutionContext.currentTicket();
        NotificationType type = NotificationType.valueOf(notificationType);

        log.info(
            "ToolExecution(triggerNotification)({}) SupportTicket(id:{}) Type({})",
            OperationalLogContext.PHASE_START,
            ticket.getId(),
            type
        );

        notificationService.createNotification(
            ticket.getCustomer().getId(),
            type,
            title,
            body,
            ticket.getId()
        );

        log.info(
            "ToolExecution(triggerNotification)({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticket.getId()
        );
    }

    private TicketMessage buildCustomerVisibleMessage(
        SupportTicket ticket,
        MessageKind messageKind,
        String content
    ) {
        return TicketMessage.builder()
                            .ticket(ticket)
                            .messageKind(messageKind)
                            .content(content)
                            .visibleToCustomer(true)
                            .build();
    }

    private TicketMessage buildInternalMessage(
        SupportTicket ticket,
        MessageKind messageKind,
        String content
    ) {
        return TicketMessage.builder()
                            .ticket(ticket)
                            .messageKind(messageKind)
                            .content(content)
                            .visibleToCustomer(false)
                            .build();
    }

    private void resolveTicket(
        SupportTicket ticket
    ) {
        ticket.setStatus(
            TicketStatus.RESOLVED
        );

        ticket.setResolvedAt(
            Instant.now()
        );

        persistTicketActivity(
            ticket
        );
    }

    private void persistTicketActivity(
        SupportTicket ticket
    ) {
        ticket.updateLastActivity();

        supportTicketRepository.save(
            ticket
        );
    }
}
