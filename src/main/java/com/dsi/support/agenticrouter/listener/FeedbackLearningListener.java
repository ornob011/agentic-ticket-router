package com.dsi.support.agenticrouter.listener;

import com.dsi.support.agenticrouter.event.FeedbackCapturedEvent;
import com.dsi.support.agenticrouter.service.learning.PatternLearningService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedbackLearningListener {

    private final PatternLearningService patternLearningService;

    @Async("ticketRoutingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedbackCaptured(FeedbackCapturedEvent event) {
        log.info(
            "FeedbackLearningEventHandle({}) feedbackId:{}",
            OperationalLogContext.PHASE_START,
            event.getFeedbackId()
        );

        patternLearningService.learnFromFeedback(event.getFeedbackId());

        log.info(
            "FeedbackLearningEventHandle({}) feedbackId:{} Outcome(patternLearningCompleted)",
            OperationalLogContext.PHASE_COMPLETE,
            event.getFeedbackId()
        );
    }
}
