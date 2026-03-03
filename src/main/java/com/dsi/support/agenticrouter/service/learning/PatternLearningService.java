package com.dsi.support.agenticrouter.service.learning;

import com.dsi.support.agenticrouter.entity.ResolutionFeedback;
import com.dsi.support.agenticrouter.entity.RoutingPattern;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.FeedbackType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.repository.ResolutionFeedbackRepository;
import com.dsi.support.agenticrouter.repository.RoutingPatternRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatternLearningService {

    private final ResolutionFeedbackRepository feedbackRepository;
    private final RoutingPatternRepository patternRepository;
    private final PatternKeywordExtractor patternKeywordExtractor;

    @Transactional
    public void learnFromFeedback(Long feedbackId) {
        log.info(
            "PatternLearning({}) feedbackId:{}",
            OperationalLogContext.PHASE_START,
            feedbackId
        );

        ResolutionFeedback feedback = feedbackRepository.findById(feedbackId)
                                                        .orElseThrow(() -> new IllegalArgumentException("Feedback not found: " + feedbackId));

        processFeedbackByType(feedback);

        log.info(
            "PatternLearning({}) feedbackId:{} feedbackType:{} Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            feedbackId,
            feedback.getFeedbackType()
        );
    }

    private void processFeedbackByType(ResolutionFeedback feedback) {
        FeedbackType feedbackType = feedback.getFeedbackType();

        switch (feedbackType) {
            case APPROVAL -> processApproval(feedback);
            case CORRECTION -> processCorrection(feedback);
            case REJECTION -> processRejection(feedback);
            case RATING -> processRating(feedback);
        }
    }

    private void processApproval(ResolutionFeedback feedback) {
        NextAction approvedAction = feedback.getOriginalAction();

        if (approvedAction == null) {
            log.warn(
                "PatternLearning({}) Cannot process approval without original action, feedbackId:{}",
                OperationalLogContext.PHASE_FAIL,
                feedback.getId()
            );
            return;
        }

        SupportTicket ticket = feedback.getTicket();
        TicketCategory category = ticket.getCurrentCategory();

        if (category == null) {
            return;
        }

        Optional<RoutingPattern> existingPattern = patternRepository.findByCategoryAndAction(
            category,
            approvedAction
        );

        if (existingPattern.isPresent()) {
            RoutingPattern pattern = existingPattern.get();
            pattern.recordSuccess();
            patternRepository.save(pattern);

            log.info(
                "PatternLearning({}) Category:{} Action:{} Outcome(patternUpdated:successIncrement)",
                OperationalLogContext.PHASE_PERSIST,
                category,
                approvedAction
            );
        } else {
            RoutingPattern newPattern = createPatternFromTicket(
                ticket,
                category,
                approvedAction
            );
            patternRepository.save(newPattern);

            log.info(
                "PatternLearning({}) Category:{} Action:{} Outcome(patternCreated)",
                OperationalLogContext.PHASE_PERSIST,
                category,
                approvedAction
            );
        }
    }

    private void processCorrection(ResolutionFeedback feedback) {
        NextAction originalAction = feedback.getOriginalAction();
        NextAction correctedAction = feedback.getCorrectedAction();

        if (originalAction == null || correctedAction == null) {
            log.warn(
                "PatternLearning({}) Cannot process correction without both actions, feedbackId:{}",
                OperationalLogContext.PHASE_FAIL,
                feedback.getId()
            );
            return;
        }

        SupportTicket ticket = feedback.getTicket();
        TicketCategory category = feedback.getCorrectedCategory();

        if (category == null) {
            category = ticket.getCurrentCategory();
        }

        if (category == null) {
            return;
        }

        recordFailureForAction(category, originalAction);

        Optional<RoutingPattern> correctedPattern = patternRepository.findByCategoryAndAction(
            category,
            correctedAction
        );

        if (correctedPattern.isPresent()) {
            RoutingPattern pattern = correctedPattern.get();
            pattern.recordSuccess();
            patternRepository.save(pattern);
        } else {
            RoutingPattern newPattern = createPatternFromTicket(
                ticket,
                category,
                correctedAction
            );
            patternRepository.save(newPattern);
        }

        log.info(
            "PatternLearning({}) Category:{} OriginalAction:{} CorrectedAction:{} Outcome(patternsUpdated)",
            OperationalLogContext.PHASE_PERSIST,
            category,
            originalAction,
            correctedAction
        );
    }

    private void processRejection(ResolutionFeedback feedback) {
        NextAction rejectedAction = feedback.getOriginalAction();

        if (rejectedAction == null) {
            log.warn(
                "PatternLearning({}) Cannot process rejection without original action, feedbackId:{}",
                OperationalLogContext.PHASE_FAIL,
                feedback.getId()
            );
            return;
        }

        SupportTicket ticket = feedback.getTicket();
        TicketCategory category = ticket.getCurrentCategory();

        if (category == null) {
            return;
        }

        recordFailureForAction(category, rejectedAction);

        log.info(
            "PatternLearning({}) Category:{} RejectedAction:{} Outcome(failureRecorded)",
            OperationalLogContext.PHASE_PERSIST,
            category,
            rejectedAction
        );
    }

    private void processRating(ResolutionFeedback feedback) {
        Integer rating = feedback.getRating();

        if (rating == null) {
            return;
        }

        NextAction action = feedback.getOriginalAction();

        if (action == null) {
            return;
        }

        SupportTicket ticket = feedback.getTicket();
        TicketCategory category = ticket.getCurrentCategory();

        if (category == null) {
            return;
        }

        if (feedback.isPositiveFeedback()) {
            recordSuccessForAction(category, action);
        } else if (feedback.isNegativeFeedback()) {
            recordFailureForAction(category, action);
        }

        log.info(
            "PatternLearning({}) Category:{} Action:{} Rating:{} Outcome(ratingProcessed)",
            OperationalLogContext.PHASE_PERSIST,
            category,
            action,
            rating
        );
    }

    private void recordSuccessForAction(TicketCategory category, NextAction action) {
        patternRepository.findByCategoryAndAction(category, action)
                         .ifPresent(pattern -> {
                             pattern.recordSuccess();
                             patternRepository.save(pattern);
                         });
    }

    private void recordFailureForAction(TicketCategory category, NextAction action) {
        Optional<RoutingPattern> existingPattern = patternRepository.findByCategoryAndAction(
            category,
            action
        );

        if (existingPattern.isPresent()) {
            RoutingPattern pattern = existingPattern.get();
            pattern.recordFailure();
            patternRepository.save(pattern);
        }
    }

    private RoutingPattern createPatternFromTicket(
        SupportTicket ticket,
        TicketCategory category,
        NextAction action
    ) {
        return RoutingPattern.builder()
                             .category(category)
                             .keywords(patternKeywordExtractor.extractKeywords(ticket.getSubject()))
                             .successfulAction(action)
                             .successCount(1)
                             .failureCount(0)
                             .confidenceBoost(0.1)
                             .active(true)
                             .build();
    }
}
