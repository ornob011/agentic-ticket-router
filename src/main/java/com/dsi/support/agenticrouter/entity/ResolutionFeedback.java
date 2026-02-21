package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.FeedbackType;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "resolution_feedback",
    indexes = {
        @Index(name = "idx_feedback_ticket", columnList = "ticket_id"),
        @Index(name = "idx_feedback_type", columnList = "feedback_type"),
        @Index(name = "idx_feedback_agent", columnList = "agent_id")
    }
)
public class ResolutionFeedback extends BaseEntity {

    private static final int NEGATIVE_RATING_THRESHOLD = 2;
    private static final int POSITIVE_RATING_THRESHOLD = 4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 20)
    private FeedbackType feedbackType;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_category", length = 30)
    private TicketCategory originalCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "corrected_category", length = 30)
    private TicketCategory correctedCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_action", length = 50)
    private NextAction originalAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "corrected_action", length = 50)
    private NextAction correctedAction;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "feedback_notes", columnDefinition = "text")
    private String feedbackNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AppUser agent;

    @Column(name = "routing_id")
    private Long routingId;

    public boolean hasCorrection() {
        return Objects.nonNull(correctedAction)
               || Objects.nonNull(correctedCategory);
    }

    public boolean isNegativeFeedback() {
        if (feedbackType == FeedbackType.REJECTION) {
            return true;
        }

        if (feedbackType == FeedbackType.CORRECTION) {
            return true;
        }

        return Objects.nonNull(rating)
               && rating <= NEGATIVE_RATING_THRESHOLD;
    }

    public boolean isPositiveFeedback() {
        if (feedbackType == FeedbackType.APPROVAL) {
            return true;
        }

        return Objects.nonNull(rating)
               && rating >= POSITIVE_RATING_THRESHOLD;
    }
}
