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
import java.util.Set;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

    List<AuditEvent> findByTicket_IdAndEventTypeInOrderByCreatedAtAsc(
        @Param("ticketId") Long ticketId,
        @Param("eventTypes") Set<AuditEventType> eventTypes
    );

    boolean existsByTicket_IdAndEventType(
        Long ticketId,
        AuditEventType eventType
    );

    boolean existsByTicket_IdAndEventTypeAndCreatedAtAfter(
        Long ticketId,
        AuditEventType eventType,
        Instant createdAt
    );

    @Query("""
        SELECT
            ae.id AS id,
            ae.eventType AS eventType,
            ae.description AS description,
            COALESCE(performedBy.fullName, 'SYSTEM') AS performedByName,
            ae.createdAt AS createdAt
        FROM AuditEvent ae
        LEFT JOIN ae.performedBy performedBy
        WHERE ae.ticket.id = :ticketId
        AND ae.eventType IN :eventTypes
        ORDER BY ae.createdAt ASC
        """)
    List<AuditEventView> findTicketAuditTrailView(
        @Param("ticketId") Long ticketId,
        @Param("eventTypes") Set<AuditEventType> eventTypes
    );

    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE (:ticketId IS NULL OR ae.ticket.id = :ticketId)
        AND (:eventType IS NULL OR ae.eventType = :eventType)
        AND (:performedById IS NULL OR ae.performedBy.id = :performedById)
        AND ae.createdAt >= COALESCE(:startDate, ae.createdAt)
        AND ae.createdAt <= COALESCE(:endDate, ae.createdAt)
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

    @Query(
        value = """
            SELECT
                ae.id AS id,
                ae.eventType AS eventType,
                ae.description AS description,
                COALESCE(performedBy.fullName, 'SYSTEM') AS performedByName,
                ae.createdAt AS createdAt
            FROM AuditEvent ae
            LEFT JOIN ae.performedBy performedBy
            WHERE (:ticketId IS NULL OR ae.ticket.id = :ticketId)
            AND (:eventType IS NULL OR ae.eventType = :eventType)
            AND (:performedById IS NULL OR ae.performedBy.id = :performedById)
            AND ae.createdAt >= COALESCE(:startDate, ae.createdAt)
            AND ae.createdAt <= COALESCE(:endDate, ae.createdAt)
            ORDER BY ae.createdAt DESC
            """,
        countQuery = """
            SELECT COUNT(ae)
            FROM AuditEvent ae
            WHERE (:ticketId IS NULL OR ae.ticket.id = :ticketId)
            AND (:eventType IS NULL OR ae.eventType = :eventType)
            AND (:performedById IS NULL OR ae.performedBy.id = :performedById)
            AND ae.createdAt >= COALESCE(:startDate, ae.createdAt)
            AND ae.createdAt <= COALESCE(:endDate, ae.createdAt)
            """
    )
    Page<AuditEventView> findByFiltersView(
        @Param("ticketId") Long ticketId,
        @Param("eventType") AuditEventType eventType,
        @Param("performedById") Long performedById,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    interface AuditEventView {

        Long getId();

        AuditEventType getEventType();

        String getDescription();

        String getPerformedByName();

        Instant getCreatedAt();
    }
}
