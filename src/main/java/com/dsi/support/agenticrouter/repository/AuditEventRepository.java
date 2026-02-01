package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.AuditEvent;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE (:ticketId IS NULL OR ae.ticket.id = :ticketId)
        AND (:eventType IS NULL OR ae.eventType = :eventType)
        AND (:performedById IS NULL OR ae.performedBy.id = :performedById)
        AND (:startDate IS NULL OR ae.createdAt >= :startDate)
        AND (:endDate IS NULL OR ae.createdAt <= :endDate)
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findByFilters(
        @Param("ticketId") Long ticketId,
        @Param("eventType") AuditEventType eventType,
        @Param("performedById") Long performedById,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );
}
