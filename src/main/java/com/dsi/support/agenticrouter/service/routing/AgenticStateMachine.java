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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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

    public void executeAction(
        SupportTicket supportTicket,
        TicketRouting ticketRouting
    ) {
        switch (ticketRouting.getNextAction()) {
            case ASSIGN_QUEUE -> assignQueue(supportTicket, ticketRouting);
            case ASK_CLARIFYING -> askClarifyingQuestion(supportTicket, ticketRouting);
            case AUTO_REPLY -> sendAutoReply(supportTicket, ticketRouting);
            case ESCALATE -> escalateTicket(supportTicket, ticketRouting);
            case HUMAN_REVIEW -> routeToHumanReview(supportTicket, ticketRouting);
        }
    }

    private void assignQueue(
        SupportTicket supportTicket,
        TicketRouting ticketRouting
    ) {
        supportTicket.setAssignedQueue(ticketRouting.getQueue());
        supportTicket.setStatus(TicketStatus.ASSIGNED);

        if (supportTicket.getFirstAssignedAt() == null) {
            supportTicket.setFirstAssignedAt(Instant.now());
        }

        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.QUEUE_ASSIGNED,
            supportTicket.getId(),
            null,
            String.format(
                "Ticket assigned to queue: %s (confidence: %s)",
                ticketRouting.getQueue(),
                ticketRouting.getConfidence()
            ),
            null
        );
    }

    private void askClarifyingQuestion(
        SupportTicket supportTicket,
        TicketRouting ticketRouting
    ) {
        if (ticketRouting.getClarifyingQuestion() == null || ticketRouting.getClarifyingQuestion().isBlank()) {
            routeToHumanReview(
                supportTicket,
                ticketRouting
            );

            return;
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .author(null)
                                                   .messageKind(MessageKind.CLARIFYING_QUESTION)
                                                   .content(ticketRouting.getClarifyingQuestion())
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);

        supportTicket.setStatus(TicketStatus.WAITING_CUSTOMER);
        supportTicketRepository.save(supportTicket);

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.NEW_MESSAGE,
            "Clarification Needed: " + supportTicket.getFormattedTicketNo(),
            "We need more information to help with your ticket.",
            supportTicket.getId()
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "System posted clarifying question",
            null
        );
    }

    private void sendAutoReply(
        SupportTicket supportTicket,
        TicketRouting ticketRouting
    ) {
        if (ticketRouting.getDraftReply() == null || ticketRouting.getDraftReply().isBlank()) {
            assignQueue(
                supportTicket,
                ticketRouting
            );
            return;
        }

        TicketMessage ticketMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .author(null)
                                                   .messageKind(MessageKind.AUTO_REPLY)
                                                   .content(ticketRouting.getDraftReply())
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(ticketMessage);

        supportTicket.setStatus(TicketStatus.RESOLVED);
        supportTicket.setResolvedAt(Instant.now());

        supportTicketRepository.save(supportTicket);

        notificationService.createNotification(
            supportTicket.getCustomer().getId(),
            NotificationType.STATUS_CHANGE,
            "Ticket Resolved: " + supportTicket.getFormattedTicketNo(),
            "Your ticket has been automatically resolved. If you need further assistance, please reply.",
            supportTicket.getId()
        );

        auditService.recordEvent(
            AuditEventType.MESSAGE_POSTED,
            supportTicket.getId(),
            null,
            "System sent auto-reply and resolved ticket",
            null
        );
    }

    private void escalateTicket(
        SupportTicket supportTicket,
        TicketRouting ticketRouting
    ) {
        String reason = String.format(
            "Auto-escalated: category=%s, queue=%s, tags=%s",
            ticketRouting.getCategory(),
            ticketRouting.getQueue(),
            ticketRouting.getRationaleTags()
        );

        Escalation escalation = Escalation.builder()
                                          .ticket(supportTicket)
                                          .reason(reason)
                                          .resolved(false)
                                          .build();

        escalationRepository.save(escalation);

        supportTicket.setStatus(TicketStatus.ESCALATED);
        supportTicket.setEscalated(true);
        supportTicket.setAssignedQueue(ticketRouting.getQueue());

        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.ESCALATION_CREATED,
            supportTicket.getId(),
            null,
            "Ticket escalated: " + reason,
            null
        );
    }

    private void routeToHumanReview(
        SupportTicket supportTicket,
        TicketRouting ticketRouting
    ) {
        supportTicket.setStatus(TicketStatus.TRIAGING);
        supportTicket.setAssignedQueue(ticketRouting.getQueue());

        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.POLICY_GATE_TRIGGERED,
            supportTicket.getId(),
            null,
            String.format(
                "Routed to human review: confidence=%s, category=%s",
                ticketRouting.getConfidence(),
                ticketRouting.getCategory()
            ),
            null
        );
    }
}
