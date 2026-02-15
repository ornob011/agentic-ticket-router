package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.AgentQueueMembership;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AgentQueueMembershipRepository extends JpaRepository<AgentQueueMembership, Long> {

    List<AgentQueueMembership> findByUserId(Long userId);

    boolean existsByUserIdAndQueue(Long userId, TicketQueue queue);

    @Query("""
        SELECT membership
        FROM AgentQueueMembership membership
        JOIN FETCH membership.user user
        ORDER BY user.username ASC, membership.queue ASC
        """)
    List<AgentQueueMembership> findAllWithUser();
}
