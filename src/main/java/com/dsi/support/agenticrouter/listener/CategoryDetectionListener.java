package com.dsi.support.agenticrouter.listener;

import com.dsi.support.agenticrouter.event.CategoryDetectionEvent;
import com.dsi.support.agenticrouter.service.ticket.CategoryDetectionEventHandlerService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryDetectionListener {

    private final CategoryDetectionEventHandlerService categoryDetectionEventHandlerService;

    @Async("ticketRoutingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCategoryDetection(
        CategoryDetectionEvent categoryDetectionEvent
    ) {
        log.info(
            "CategoryDetectionEventHandle({}) SupportTicket(id:{}) Actor(id:{}) Outcome(replyLength:{})",
            OperationalLogContext.PHASE_START,
            categoryDetectionEvent.getTicketId(),
            categoryDetectionEvent.getCustomerId(),
            StringUtils.length(categoryDetectionEvent.getReplyContent())
        );

        categoryDetectionEventHandlerService.handleCategoryDetection(
            categoryDetectionEvent
        );

        log.info(
            "CategoryDetectionEventHandle({}) SupportTicket(id:{}) Outcome(handler:{})",
            OperationalLogContext.PHASE_COMPLETE,
            categoryDetectionEvent.getTicketId(),
            "executed"
        );
    }
}
