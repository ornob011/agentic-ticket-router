package com.dsi.support.agenticrouter.service.learning;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.ResolutionFeedback;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.FeedbackType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.event.FeedbackCapturedEvent;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.ResolutionFeedbackRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackCaptureService {

    private final ResolutionFeedbackRepository feedbackRepository;
    private final SupportTicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppUserRepository appUserRepository;

    private static String enumName(
        Enum<?> value
    ) {
        return Objects.nonNull(value)
            ? value.name()
            : null;
    }

    @Transactional
    public ApiDtos.FeedbackResponse submitFeedback(
        ApiDtos.FeedbackRequest request
    ) throws BindException {
        log.info(
            "FeedbackSubmit({}) ticketId:{} feedbackType:{} rating:{}",
            OperationalLogContext.PHASE_START,
            request.ticketId(),
            request.feedbackType(),
            request.rating()
        );

        validateOriginalActionRequirement(
            request
        );

        validateRoutingIdRequirement(
            request
        );

        validateCorrectedActionRequirement(
            request
        );

        Long actorId = Utils.getLoggedInUserId();

        AppUser actor = appUserRepository.findById(actorId)
                                         .orElseThrow(
                                             DataNotFoundException.supplier(
                                                 AppUser.class,
                                                 actorId
                                             )
                                         );

        SupportTicket ticket = ticketRepository.getReferenceById(
            request.ticketId()
        );

        ResolutionFeedback savedFeedback = switch (request.feedbackType()) {
            case RATING -> captureRating(
                ticket,
                request.ticketId(),
                request.routingId(),
                request.rating(),
                request.notes(),
                actor
            );
            case CORRECTION -> captureCorrection(
                ticket,
                request.ticketId(),
                toTicketCategory(
                    request.originalCategory()
                ),
                toTicketCategory(
                    request.correctedCategory()
                ),
                toNextAction(
                    request.originalAction()
                ),
                toNextAction(
                    request.correctedAction()
                ),
                request.routingId(),
                request.notes(),
                actor
            );
            case REJECTION -> captureRejection(
                ticket,
                request.ticketId(),
                toNextAction(
                    request.originalAction()
                ),
                request.routingId(),
                request.notes(),
                actor
            );
            case APPROVAL -> captureApproval(
                ticket,
                request.ticketId(),
                toNextAction(
                    request.originalAction()
                ),
                request.routingId(),
                request.notes(),
                actor
            );
        };

        publishFeedbackCapturedEvent(
            savedFeedback
        );

        log.info(
            "FeedbackSubmit({}) ticketId:{} feedbackType:{} Outcome(feedbackId:{},agentId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            request.ticketId(),
            request.feedbackType(),
            savedFeedback.getId(),
            actor.getId()
        );

        return buildFeedbackResponse(
            savedFeedback,
            actor
        );
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.FeedbackResponse> getFeedbackForTicket(
        Long ticketId
    ) {
        log.info(
            "FeedbackTicketGet({}) ticketId:{}",
            OperationalLogContext.PHASE_START,
            ticketId
        );

        List<ApiDtos.FeedbackResponse> result = feedbackRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                                                                  .stream()
                                                                  .map(feedback -> buildFeedbackResponse(
                                                                      feedback,
                                                                      null
                                                                  ))
                                                                  .toList();

        log.info(
            "FeedbackTicketGet({}) ticketId:{} Outcome(count:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            result.size()
        );

        return result;
    }

    @Transactional(readOnly = true)
    public ApiDtos.FeedbackSummary getFeedbackSummaryForTicket(
        Long ticketId
    ) {
        log.info(
            "FeedbackSummaryGet({}) ticketId:{}",
            OperationalLogContext.PHASE_START,
            ticketId
        );

        List<ResolutionFeedback> feedbacks = feedbackRepository.findByTicketIdOrderByCreatedAtDesc(
            ticketId
        );

        ApiDtos.FeedbackSummary summary = buildFeedbackSummary(
            ticketId,
            feedbacks
        );

        log.info(
            "FeedbackSummaryGet({}) ticketId:{} Outcome(total:{},approval:{},correction:{},rejection:{},avgRating:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            summary.totalFeedbackCount(),
            summary.approvalCount(),
            summary.correctionCount(),
            summary.rejectionCount(),
            summary.averageRating()
        );

        return summary;
    }

    public Double getAverageRatingForTicket(
        Long ticketId
    ) {
        return feedbackRepository.getAverageRatingForTicket(ticketId);
    }

    private ResolutionFeedback captureRating(
        SupportTicket ticket,
        Long ticketId,
        Long routingId,
        Integer rating,
        String notes,
        AppUser actor
    ) {
        ResolutionFeedback feedback = feedbackRepository.findTopByTicketIdAndAgentIdAndFeedbackTypeOrderByCreatedAtDesc(
            ticketId,
            actor.getId(),
            FeedbackType.RATING
        ).orElseGet(() -> ResolutionFeedback.builder()
                                            .ticket(ticket)
                                            .feedbackType(FeedbackType.RATING)
                                            .agent(actor)
                                            .build());

        feedback.setRating(
            rating
        );

        feedback.setFeedbackNotes(
            notes
        );

        feedback.setRoutingId(
            routingId
        );

        ResolutionFeedback savedFeedback = feedbackRepository.save(
            feedback
        );

        log.info(
            "FeedbackCaptureRating({}) ticketId:{} rating:{} Outcome(feedbackId:{},agentId:{})",
            OperationalLogContext.PHASE_PERSIST,
            ticketId,
            rating,
            savedFeedback.getId(),
            actor.getId()
        );

        return savedFeedback;
    }

    private ResolutionFeedback captureCorrection(
        SupportTicket ticket,
        Long ticketId,
        TicketCategory originalCategory,
        TicketCategory correctedCategory,
        NextAction originalAction,
        NextAction correctedAction,
        Long routingId,
        String notes,
        AppUser actor
    ) {
        ResolutionFeedback feedback = ResolutionFeedback.builder()
                                                        .ticket(ticket)
                                                        .feedbackType(FeedbackType.CORRECTION)
                                                        .originalCategory(originalCategory)
                                                        .correctedCategory(correctedCategory)
                                                        .originalAction(originalAction)
                                                        .correctedAction(correctedAction)
                                                        .feedbackNotes(notes)
                                                        .routingId(routingId)
                                                        .agent(actor)
                                                        .build();

        ResolutionFeedback savedFeedback = feedbackRepository.save(
            feedback
        );

        log.info(
            "FeedbackCaptureCorrection({}) ticketId:{} Outcome(originalAction:{},correctedAction:{},feedbackId:{},agentId:{})",
            OperationalLogContext.PHASE_PERSIST,
            ticketId,
            originalAction,
            correctedAction,
            savedFeedback.getId(),
            actor.getId()
        );

        return savedFeedback;
    }

    private ResolutionFeedback captureRejection(
        SupportTicket ticket,
        Long ticketId,
        NextAction originalAction,
        Long routingId,
        String reason,
        AppUser actor
    ) {
        ResolutionFeedback feedback = feedbackRepository.findTopByRoutingIdAndAgentIdAndFeedbackTypeOrderByCreatedAtDesc(
            routingId,
            actor.getId(),
            FeedbackType.REJECTION
        ).orElseGet(() -> ResolutionFeedback.builder()
                                            .ticket(ticket)
                                            .feedbackType(FeedbackType.REJECTION)
                                            .agent(actor)
                                            .build());

        feedback.setOriginalAction(
            originalAction
        );

        feedback.setFeedbackNotes(
            reason
        );

        feedback.setRoutingId(
            routingId
        );

        ResolutionFeedback savedFeedback = feedbackRepository.save(
            feedback
        );

        log.info(
            "FeedbackCaptureRejection({}) ticketId:{} Outcome(originalAction:{},feedbackId:{},agentId:{})",
            OperationalLogContext.PHASE_PERSIST,
            ticketId,
            originalAction,
            savedFeedback.getId(),
            actor.getId()
        );

        return savedFeedback;
    }

    private ResolutionFeedback captureApproval(
        SupportTicket ticket,
        Long ticketId,
        NextAction approvedAction,
        Long routingId,
        String notes,
        AppUser actor
    ) {
        ResolutionFeedback feedback = feedbackRepository.findTopByRoutingIdAndAgentIdAndFeedbackTypeOrderByCreatedAtDesc(
            routingId,
            actor.getId(),
            FeedbackType.APPROVAL
        ).orElseGet(() -> ResolutionFeedback.builder()
                                            .ticket(ticket)
                                            .feedbackType(FeedbackType.APPROVAL)
                                            .agent(actor)
                                            .build());

        feedback.setOriginalAction(
            approvedAction
        );

        feedback.setFeedbackNotes(
            notes
        );

        feedback.setRoutingId(
            routingId
        );

        ResolutionFeedback savedFeedback = feedbackRepository.save(
            feedback
        );

        log.info(
            "FeedbackCaptureApproval({}) ticketId:{} Outcome(action:{},feedbackId:{},agentId:{})",
            OperationalLogContext.PHASE_PERSIST,
            ticketId,
            approvedAction,
            savedFeedback.getId(),
            actor.getId()
        );

        return savedFeedback;
    }

    private void publishFeedbackCapturedEvent(
        ResolutionFeedback savedFeedback
    ) {
        eventPublisher.publishEvent(
            new FeedbackCapturedEvent(
                savedFeedback.getId()
            )
        );
    }

    private ApiDtos.FeedbackResponse buildFeedbackResponse(
        ResolutionFeedback feedback,
        AppUser actorOverride
    ) {
        String agentName = resolveAgentName(
            feedback,
            actorOverride
        );

        return ApiDtos.FeedbackResponse.builder()
                                       .id(feedback.getId())
                                       .ticketId(feedback.getTicket().getId())
                                       .feedbackType(feedback.getFeedbackType())
                                       .rating(feedback.getRating())
                                       .originalCategory(enumName(
                                           feedback.getOriginalCategory()
                                       ))
                                       .correctedCategory(enumName(
                                           feedback.getCorrectedCategory()
                                       ))
                                       .originalAction(enumName(
                                           feedback.getOriginalAction()
                                       ))
                                       .correctedAction(enumName(
                                           feedback.getCorrectedAction()
                                       ))
                                       .notes(feedback.getFeedbackNotes())
                                       .agentName(agentName)
                                       .createdAt(feedback.getCreatedAt())
                                       .build();
    }

    private String resolveAgentName(
        ResolutionFeedback feedback,
        AppUser actorOverride
    ) {
        if (Objects.nonNull(actorOverride)) {
            return actorOverride.getFullName();
        }

        if (Objects.nonNull(feedback.getAgent())) {
            return feedback.getAgent().getFullName();
        }

        return null;
    }

    private ApiDtos.FeedbackSummary buildFeedbackSummary(
        Long ticketId,
        List<ResolutionFeedback> feedbacks
    ) {
        return ApiDtos.FeedbackSummary.builder()
                                      .ticketId(ticketId)
                                      .totalFeedbackCount(feedbacks.size())
                                      .approvalCount(countByType(feedbacks, FeedbackType.APPROVAL))
                                      .correctionCount(countByType(feedbacks, FeedbackType.CORRECTION))
                                      .rejectionCount(countByType(feedbacks, FeedbackType.REJECTION))
                                      .averageRating(getAverageRatingForTicket(ticketId))
                                      .build();
    }

    private TicketCategory toTicketCategory(
        String value
    ) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        return TicketCategory.valueOf(value);
    }

    private NextAction toNextAction(
        String value
    ) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        return NextAction.valueOf(value);
    }

    private void validateOriginalActionRequirement(
        ApiDtos.FeedbackRequest request
    ) throws BindException {
        if (request.feedbackType() == FeedbackType.RATING) {
            return;
        }

        if (StringUtils.isBlank(request.originalAction())) {
            throw BindValidation.fieldError(
                "feedbackRequest",
                "originalAction",
                String.format(
                    "Original action is required for %s feedback",
                    request.feedbackType()
                )
            );
        }
    }

    private void validateRoutingIdRequirement(
        ApiDtos.FeedbackRequest request
    ) throws BindException {
        if (request.feedbackType() == FeedbackType.APPROVAL
            || request.feedbackType() == FeedbackType.REJECTION) {
            if (Objects.isNull(request.routingId())) {
                throw BindValidation.fieldError(
                    "feedbackRequest",
                    "routingId",
                    String.format(
                        "Routing id is required for %s feedback",
                        request.feedbackType()
                    )
                );
            }
        }
    }

    private void validateCorrectedActionRequirement(
        ApiDtos.FeedbackRequest request
    ) throws BindException {
        if (request.feedbackType() == FeedbackType.CORRECTION
            && StringUtils.isBlank(request.correctedAction())) {
            throw BindValidation.fieldError(
                "feedbackRequest",
                "correctedAction",
                "Corrected action is required for CORRECTION feedback"
            );
        }
    }

    private long countByType(
        List<ResolutionFeedback> feedbacks,
        FeedbackType feedbackType
    ) {
        return feedbacks.stream()
                        .filter(feedback -> feedback.getFeedbackType() == feedbackType)
                        .count();
    }
}
