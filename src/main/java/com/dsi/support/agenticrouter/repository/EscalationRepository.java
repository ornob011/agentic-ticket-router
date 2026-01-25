package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    long countByResolvedFalse();

    List<Escalation> findTop10ByResolvedFalseOrderByCreatedAtDesc();
}
