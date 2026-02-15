package com.dsi.support.agenticrouter.listener;

import com.dsi.support.agenticrouter.event.CategoryDetectionEvent;
import com.dsi.support.agenticrouter.service.ticket.CategoryDetectionEventHandlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CategoryDetectionListener {

    private final CategoryDetectionEventHandlerService categoryDetectionEventHandlerService;

    @Async("ticketRoutingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCategoryDetection(
        CategoryDetectionEvent categoryDetectionEvent
    ) {
        categoryDetectionEventHandlerService.handleCategoryDetection(

        );
    }
}
