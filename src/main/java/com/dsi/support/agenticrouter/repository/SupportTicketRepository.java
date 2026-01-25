package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findTop5ByCustomerIdOrderByLastActivityAtDesc(
        Long customerId
    );

    List<SupportTicket> findTop5ByAssignedAgentIdOrderByLastActivityAtDesc(
        Long assignedAgentId
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

    interface TicketQueueCount {

        TicketQueue getQueue();

        long getCount();
    }

    @Query("""
        SELECT
            ticket.assignedQueue AS queue,
            COUNT(ticket) AS count
        FROM SupportTicket ticket
        WHERE ticket.assignedQueue IS NOT NULL
        GROUP BY ticket.assignedQueue
        """)
    List<TicketQueueCount> countTicketsGroupedByAssignedQueue();

    interface AgentTicketCounts {

        long getTotalCount();

        long getInProgressCount();

        long getWaitingCustomerCount();
    }

    @Query("""
        SELECT
            COUNT(ticket) AS totalCount,
            SUM(
                CASE
                    WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.IN_PROGRESS
                    THEN 1
                    ELSE 0
                END
            ) AS inProgressCount,
            SUM(
                CASE
                    WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.WAITING_CUSTOMER
                    THEN 1
                    ELSE 0
                END
            ) AS waitingCustomerCount
        FROM SupportTicket ticket
        WHERE ticket.assignedAgent.id = :assignedAgentId
        """)
    AgentTicketCounts countAgentTicketCounts(
        @Param("assignedAgentId") Long assignedAgentId
    );

    interface CustomerTicketCounts {

        long getOpenCount();

        long getWaitingCustomerCount();

        long getResolvedCount();

        long getClosedCount();
    }

    @Query("""
        SELECT
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
            ) AS openCount,
            SUM(
                CASE
                    WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.WAITING_CUSTOMER
                    THEN 1
                    ELSE 0
                END
            ) AS waitingCustomerCount,
            SUM(
                CASE
                    WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.RESOLVED
                    THEN 1
                    ELSE 0
                END
            ) AS resolvedCount,
            SUM(
                CASE
                    WHEN ticket.status = com.dsi.support.agenticrouter.enums.TicketStatus.CLOSED
                    THEN 1
                    ELSE 0
                END
            ) AS closedCount
        FROM SupportTicket ticket
        WHERE ticket.customer.id = :customerId
        """)
    CustomerTicketCounts countCustomerTicketCounts(
        @Param("customerId") Long customerId
    );
}
