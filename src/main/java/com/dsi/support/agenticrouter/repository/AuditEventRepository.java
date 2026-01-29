package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
