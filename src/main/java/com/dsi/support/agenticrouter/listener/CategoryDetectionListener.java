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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

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
    public void handleCategoryDetection(CategoryDetectionEvent event) {
        supportTicketRepository.findById(event.getTicketId()).ifPresent(supportTicket -> {
            if (Objects.nonNull(supportTicket.getCurrentCategory())) {
                MessageCategoryService.CategoryDetectionResult detectionResult = messageCategoryService.detectCategory(event.getReplyContent(), supportTicket);

                if (detectionResult.isSuccess() &&
                    !messageCategoryService.isSameCategory(
                        supportTicket.getCurrentCategory(),
                        detectionResult.getDetectedCategory()
                    )) {

                    processCategoryMismatch(
                        supportTicket,
                        event.getReplyContent(),
                        detectionResult.getDetectedCategory(),
                        event.getCustomerId()
                    );
                }
            }
        });
    }

    private void processCategoryMismatch(
        SupportTicket supportTicket,
        String replyContent,
        TicketCategory detectedCategory,
        Long customerId
    ) {
        TicketMessage customerMessage = TicketMessage.builder()
                                                     .ticket(supportTicket)
                                                     .content(replyContent)
                                                     .messageKind(MessageKind.CUSTOMER_MESSAGE)
                                                     .author(supportTicket.getCustomer())
                                                     .visibleToCustomer(true)
                                                     .build();

        ticketMessageRepository.save(customerMessage);

        TicketMessage systemMessage = TicketMessage.builder()
                                                   .ticket(supportTicket)
                                                   .author(null)
                                                   .messageKind(MessageKind.SYSTEM_MESSAGE)
                                                   .content(String.format(
                                                       "Your message appears to be about a different topic (%s) than the original ticket (%s). " +
                                                       "Please create a new support ticket for this inquiry. " +
                                                       "Each ticket should address a single topic for faster resolution.",
                                                       detectedCategory.name(),
                                                       supportTicket.getCurrentCategory().name()
                                                   ))
                                                   .visibleToCustomer(true)
                                                   .build();

        ticketMessageRepository.save(systemMessage);

        supportTicket.updateLastActivity();
        supportTicket.setCurrentCategory(detectedCategory);
        supportTicket.setStatus(TicketStatus.CLOSED);

        supportTicketRepository.save(supportTicket);

        auditService.recordEvent(
            AuditEventType.POLICY_GATE_TRIGGERED,
            supportTicket.getId(),
            null,
            String.format(
                "Blocked reply: category mismatch detected. Reply category: %s, Ticket category: %s",
                detectedCategory,
                supportTicket.getCurrentCategory()
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
}
