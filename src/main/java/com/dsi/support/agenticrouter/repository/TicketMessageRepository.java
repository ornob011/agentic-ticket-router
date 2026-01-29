package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
