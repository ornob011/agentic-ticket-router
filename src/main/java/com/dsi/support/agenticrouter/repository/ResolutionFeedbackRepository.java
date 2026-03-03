package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.ResolutionFeedback;
import com.dsi.support.agenticrouter.enums.FeedbackType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResolutionFeedbackRepository extends JpaRepository<ResolutionFeedback, Long> {

    List<ResolutionFeedback> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    List<ResolutionFeedback> findByAgentIdOrderByCreatedAtDesc(Long agentId);

    List<ResolutionFeedback> findByFeedbackType(FeedbackType feedbackType);

    Optional<ResolutionFeedback> findTopByTicketIdAndAgentIdAndFeedbackTypeOrderByCreatedAtDesc(
        Long ticketId,
        Long agentId,
        FeedbackType feedbackType
    );

    Optional<ResolutionFeedback> findTopByRoutingIdAndAgentIdAndFeedbackTypeOrderByCreatedAtDesc(
        Long routingId,
        Long agentId,
        FeedbackType feedbackType
    );

    @Query("""
        SELECT COUNT(f)
        FROM ResolutionFeedback f
        WHERE f.ticket.id = :ticketId
        AND f.feedbackType = :feedbackType
        """)
    long countByTicketIdAndFeedbackType(
        @Param("ticketId") Long ticketId,
        @Param("feedbackType") FeedbackType feedbackType
    );

    @Query("""
        SELECT AVG(f.rating)
        FROM ResolutionFeedback f
        WHERE f.ticket.id = :ticketId
        AND f.rating IS NOT NULL
        """)
    Double getAverageRatingForTicket(@Param("ticketId") Long ticketId);

    List<ResolutionFeedback> findByCorrectedActionIsNotNullOrCorrectedCategoryIsNotNullOrderByCreatedAtDesc();

}
