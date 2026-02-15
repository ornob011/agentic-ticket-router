package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Escalation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    long countByResolvedFalse();

    long countByResolvedTrue();

    List<Escalation> findTop10ByResolvedFalseOrderByCreatedAtDesc();

    @Query("""
        SELECT escalation
        FROM Escalation escalation
        JOIN escalation.ticket ticket
        WHERE escalation.resolved = false
        ORDER BY ticket.createdAt DESC
        """)
    List<Escalation> findPendingEscalations();

    Page<Escalation> findByResolvedFalse(Pageable pageable);

    Page<Escalation> findByResolvedTrue(Pageable pageable);

    Page<Escalation> findAll(Pageable pageable);

    Optional<Escalation> findByTicketId(Long ticketId);
}
