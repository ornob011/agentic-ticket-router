package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.TicketRouting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRoutingRepository extends JpaRepository<TicketRouting, Long> {

    List<TicketRouting> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
