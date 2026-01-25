package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
