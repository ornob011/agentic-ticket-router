package com.dsi.support.agenticrouter.listener;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.event.CategoryDetectionEvent;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.MessageCategoryService;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryDetectionListener {

    private final MessageCategoryService messageCategoryService;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Async("ticketRoutingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCategoryDetection(
        CategoryDetectionEvent categoryDetectionEvent
    ) {
        Long ticketId = categoryDetectionEvent.getTicketId();

        Long customerId = categoryDetectionEvent.getCustomerId();

        String replyContent = categoryDetectionEvent.getReplyContent();

        log.info(
            "CategoryDetection({}) SupportTicket(id:{}) Actor(id:{}) Outcome(replyLength:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            customerId,
            Objects.nonNull(replyContent) ? replyContent.length() : 0
        );

        Optional<SupportTicket> supportTicketOpt = supportTicketRepository.findById(ticketId);
        if (supportTicketOpt.isEmpty()) {
            log.warn(
                "CategoryDetection({}) SupportTicket(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                ticketId,
                "ticket_not_found"
            );

            return;
        }

        SupportTicket supportTicket = supportTicketOpt.get();

        if (!supportTicket.getStatus().isClosedForReplies()) {
            log.debug(
                "CategoryDetection({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "ticket_open_for_reply"
            );

            return;
        }

        TicketCategory currentCategory = supportTicket.getCurrentCategory();

        if (Objects.isNull(currentCategory)) {
            log.debug(
                "CategoryDetection({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "missing_current_category"
            );

            return;
        }

        MessageCategoryService.CategoryDetectionResult detectionResult = messageCategoryService.detectCategory(
            replyContent,
            supportTicket.getId()
        );

        if (!detectionResult.isSuccess()) {
            log.warn(
                "CategoryDetection({}) SupportTicket(id:{},status:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "category_detection_unsuccessful"
            );

            return;
        }

        TicketCategory detectedCategory = detectionResult.getDetectedCategory();

        log.info(
            "CategoryDetection({}) SupportTicket(id:{},status:{}) Outcome(currentCategory:{},detectedCategory:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            currentCategory,
            detectedCategory
        );

        if (messageCategoryService.isSameCategory(currentCategory, detectedCategory)) {
            processSameCategoryBlockedReply(
                supportTicket,
                currentCategory,
                customerId
            );

            log.info(
                "CategoryDetection({}) SupportTicket(id:{},status:{}) Outcome(action:{})",
                OperationalLogContext.PHASE_COMPLETE,
                supportTicket.getId(),
                supportTicket.getStatus(),
                "same_category_block_reply"
            );

            return;
        }

        processCategoryMismatch(
            supportTicket,
            currentCategory,
            detectedCategory,
            customerId
        );

        log.info(
            "CategoryDetection({}) SupportTicket(id:{},status:{}) Outcome(action:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            "category_mismatch_block_reply"
        );
    }

    private void processSameCategoryBlockedReply(
        SupportTicket supportTicket,
        TicketCategory category,
        Long customerId
    ) {
        String message = String.format(
            "We received your reply, but this ticket is closed and can’t accept further messages. " +
            "Your reply matches the ticket topic (%s). " +
            "Please create a NEW ticket to continue this request.",
            formatCategory(category)
        );

        saveSystemMessage(
            supportTicket,
            message
        );

        supportTicket.updateLastActivity();
        supportTicket.setStatus(TicketStatus.CLOSED);
        supportTicketRepository.save(supportTicket);

        log.info(
            "CategoryDetectionBlockedReply({}) SupportTicket(id:{},status:{}) Outcome(category:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            category
        );

        auditService.recordEvent(
            AuditEventType.POLICY_GATE_TRIGGERED,
            supportTicket.getId(),
            null,
            String.format("Blocked reply: ticket not reusable (category matched: %s)", category),
            null
        );

        notificationService.createNotification(
            customerId,
            NotificationType.TICKET_ACK,
            "Ticket Closed: " + supportTicket.getFormattedTicketNo(),
            "This ticket is closed and can’t accept replies. Please create a new ticket to continue.",
            supportTicket.getId()
        );
    }

    private void processCategoryMismatch(
        SupportTicket supportTicket,
        TicketCategory originalCategory,
        TicketCategory detectedCategory,
        Long customerId
    ) {
        String message = String.format(
            "We received your reply, but this ticket is closed and can’t accept further messages. " +
            "Your reply appears to be about %s, while this ticket was created for %s. " +
            "Please create a NEW ticket for %s so we can help you properly.",
            formatCategory(detectedCategory),
            formatCategory(originalCategory),
            formatCategory(detectedCategory)
        );

        saveSystemMessage(
            supportTicket,
            message
        );

        supportTicket.updateLastActivity();
        supportTicket.setStatus(TicketStatus.CLOSED);

        supportTicketRepository.save(supportTicket);

        log.info(
            "CategoryDetectionBlockedReply({}) SupportTicket(id:{},status:{}) Outcome(originalCategory:{},detectedCategory:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getStatus(),
            originalCategory,
            detectedCategory
        );

        auditService.recordEvent(
            AuditEventType.POLICY_GATE_TRIGGERED,
            supportTicket.getId(),
            null,
            String.format(
                "Blocked reply: category mismatch. Reply category: %s, Ticket category: %s",
                detectedCategory,
                originalCategory
            ),
            null
        );

        notificationService.createNotification(
            customerId,
            NotificationType.STATUS_CHANGE,
            "Topic Mismatch: " + supportTicket.getFormattedTicketNo(),
            "Your reply was about a different topic. Please create a new ticket for this inquiry.",
            supportTicket.getId()
        );
    }

    private void saveSystemMessage(
        SupportTicket supportTicket,
        String content
    ) {
        TicketMessage systemMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .author(null)
                                                   .messageKind(MessageKind.SYSTEM_MESSAGE)
                                                   .content(content)
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(systemMessage);
    }

    private String formatCategory(
        TicketCategory ticketCategory
    ) {
        return ticketCategory.name()
                             .replace('_', ' ');
    }

}
