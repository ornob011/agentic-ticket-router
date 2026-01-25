package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
}
