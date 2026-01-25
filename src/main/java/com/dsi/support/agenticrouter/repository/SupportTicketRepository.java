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

    boolean existsByTicketNo(Long ticketNo);

    long countByCustomerIdAndStatus(
        Long customerId,
        TicketStatus status
    );

    List<SupportTicket> findTop5ByCustomerIdOrderByLastActivityAtDesc(
        Long customerId
    );

    long countByAssignedQueue(
        TicketQueue assignedQueue
    );

    long countByAssignedAgentId(
        Long assignedAgentId
    );

    List<SupportTicket> findTop5ByAssignedAgentIdOrderByLastActivityAtDesc(
        Long assignedAgentId
    );
    
    long countByAssignedAgentIdAndStatus(
        Long assignedAgentId,
        TicketStatus status
    );

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status IN ('ASSIGNED', 'IN_PROGRESS', 'ESCALATED') AND t.lastActivityAt < :slaThreshold")
    long countSlaBreaches(@Param("slaThreshold") Instant slaThreshold);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.latestRoutingConfidence >= :threshold")
    long countHighConfidenceRoutings(@Param("threshold") BigDecimal threshold);
}
