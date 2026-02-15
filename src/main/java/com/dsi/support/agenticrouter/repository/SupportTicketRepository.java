package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Optional<SupportTicket> findTicketDetailById(
        Long id
    );

    List<SupportTicket> findTop5ByCustomerIdOrderByLastActivityAtDesc(
        Long customerId
    );

    List<SupportTicket> findTop5ByAssignedAgentIdOrderByLastActivityAtDesc(
        Long assignedAgentId
    );

    List<SupportTicket> findByLastActivityAtBeforeAndStatusIn(
        Instant before,
        List<TicketStatus> statuses
    );

    List<SupportTicket> findByStatusInAndLastActivityAtBefore(
        EnumSet<TicketStatus> statuses,
        Instant before
    );

    @Query("""
        SELECT COUNT(ticket)
        FROM SupportTicket ticket
        WHERE ticket.status IN (
            com.dsi.support.agenticrouter.enums.TicketStatus.ASSIGNED,
            com.dsi.support.agenticrouter.enums.TicketStatus.IN_PROGRESS,
            com.dsi.support.agenticrouter.enums.TicketStatus.ESCALATED
        )
        AND ticket.lastActivityAt < :slaThreshold
        """)
    long countSlaBreaches(
        @Param("slaThreshold") Instant slaThreshold
    );

    @Query("""
        SELECT COUNT(t)
        FROM SupportTicket t
        WHERE t.latestRoutingConfidence >= :threshold
        """)
    long countHighConfidenceRoutings(
        @Param("threshold") BigDecimal threshold
    );

    @Query("""
        SELECT
            ticket.assignedQueue AS queue,
            COUNT(ticket) AS count
        FROM SupportTicket ticket
        WHERE ticket.assignedQueue IS NOT NULL
        GROUP BY ticket.assignedQueue
        """)
    List<TicketQueueCount> countTicketsGroupedByAssignedQueue();

    @Query("""
        SELECT
            COUNT(ticket) AS totalCount,
            COALESCE(
                SUM(
                    CASE
                        WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.IN_PROGRESS
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS inProgressCount,
            COALESCE(
                SUM(
                    CASE
                        WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.WAITING_CUSTOMER
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS waitingCustomerCount
        FROM SupportTicket ticket
        WHERE ticket.assignedAgent.id = :assignedAgentId
        """)
    AgentTicketCounts countAgentTicketCounts(
        @Param("assignedAgentId") Long assignedAgentId
    );

    @Query("""
        SELECT
            COALESCE(
                SUM(
                    CASE
                        WHEN ticket.status IN (
                            com.dsi.support.agenticrouter.enums.TicketStatus.RECEIVED,
                            com.dsi.support.agenticrouter.enums.TicketStatus.TRIAGING,
                            com.dsi.support.agenticrouter.enums.TicketStatus.ASSIGNED,
                            com.dsi.support.agenticrouter.enums.TicketStatus.IN_PROGRESS
                        )
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS openCount,
            COALESCE(
                SUM(
                    CASE
                        WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.WAITING_CUSTOMER
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS waitingCustomerCount,
            COALESCE(
                SUM(
                    CASE
                        WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.RESOLVED
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS resolvedCount,
            COALESCE(
                SUM(
                    CASE
                        WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.CLOSED
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS closedCount
        FROM SupportTicket ticket
        WHERE ticket.customer.id = :customerId
        """)
    CustomerTicketCounts countCustomerTicketCounts(
        @Param("customerId") Long customerId
    );

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<SupportTicket> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<SupportTicket> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, TicketStatus status, Pageable pageable);

    boolean existsByIdAndCustomerId(Long id, Long customerId);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<SupportTicket> findByAssignedAgentIdOrderByLastActivityAtDesc(Long assignedAgentId, Pageable pageable);
    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<SupportTicket> findByAssignedAgentIdAndStatusOrderByLastActivityAtDesc(Long assignedAgentId, TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    @Query("""
            select supportTicket
            from SupportTicket supportTicket
            where (:queue is null or supportTicket.assignedQueue = :queue)
              and (
                    (:status is not null and supportTicket.status = :status)
                    or (:status is null and supportTicket.status in :defaultStatuses)
                  )
            order by supportTicket.currentPriority desc, supportTicket.lastActivityAt asc
        """)
    Page<SupportTicket> findQueueTickets(
        @Param("queue") TicketQueue ticketQueue,
        @Param("status") TicketStatus ticketStatus,
        @Param("defaultStatuses") Set<TicketStatus> defaultStatuses,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    @Query("""
        SELECT supportTicket
        FROM SupportTicket supportTicket
        WHERE supportTicket.assignedAgent.id IS NULL
          AND supportTicket.assignedQueue = :queue
          AND supportTicket.status IN :statuses
        ORDER BY supportTicket.currentPriority DESC, supportTicket.createdAt ASC
        """)
    Page<SupportTicket> findUnassignedTicketsInQueue(
        @Param("queue") TicketQueue queue,
        @Param("statuses") List<TicketStatus> statuses,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<SupportTicket> findByRequiresHumanReviewTrue(
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<SupportTicket> findByRequiresHumanReviewTrueAndStatusOrderByLastActivityAtDesc(
        TicketStatus status,
        Pageable pageable
    );

    long countByRequiresHumanReviewTrueAndStatus(
        TicketStatus status
    );

    @Query("""
        SELECT COUNT(ticket)
        FROM SupportTicket ticket
        WHERE ticket.assignedAgent.id = :agentId
        """)
    long countAssignedTicketsInQueue(
        @Param("agentId") Long agentId
    );

    @Query("""
        SELECT COUNT(ticket)
        FROM SupportTicket ticket
        WHERE ticket.assignedAgent.id = :agentId
        AND ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.IN_PROGRESS
        """)
    long countInProgressTickets(
        @Param("agentId") Long agentId
    );

    @Query("""
        SELECT COUNT(ticket)
        FROM SupportTicket ticket
        WHERE ticket.assignedAgent.id = :agentId
        AND ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.RESOLVED
        """)
    long countResolvedTickets(
        @Param("agentId") Long agentId
    );

    @Query("""
        SELECT COUNT(ticket)
        FROM SupportTicket ticket
        WHERE ticket.assignedAgent.id = :agentId
        AND ticket.escalated = true
        """)
    long countEscalatedTickets(
        @Param("agentId") Long agentId
    );

    @Query("""
        SELECT COUNT(ticket)
        FROM SupportTicket ticket
        WHERE ticket.assignedAgent.id = :agentId
        AND ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.WAITING_CUSTOMER
        """)
    long countAwaitingCustomerTickets(
        @Param("agentId") Long agentId
    );

    @Query("""
        SELECT COUNT(ticket)
        FROM SupportTicket ticket
        WHERE ticket.assignedAgent.id = :agentId
        AND ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.TRIAGING
        """)
    long countTriagingTickets(
        @Param("agentId") Long agentId
    );

    interface TicketQueueCount {

        TicketQueue getQueue();

        long getCount();
    }

    interface AgentTicketCounts {

        long getTotalCount();

        long getInProgressCount();

        long getWaitingCustomerCount();
    }

    interface CustomerTicketCounts {

        long getOpenCount();

        long getWaitingCustomerCount();

        long getResolvedCount();

        long getClosedCount();
    }
}
