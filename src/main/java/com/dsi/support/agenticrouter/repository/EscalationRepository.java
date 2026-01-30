package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    long countByResolvedFalse();

    List<Escalation> findTop10ByResolvedFalseOrderByCreatedAtDesc();

    @Query("""
        SELECT escalation
        FROM Escalation escalation
        JOIN escalation.ticket ticket
        WHERE escalation.resolved = false
        ORDER BY ticket.createdAt DESC
        """)
    List<Escalation> findPendingEscalations();
}
