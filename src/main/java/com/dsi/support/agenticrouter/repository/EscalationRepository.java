package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Escalation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    long countByResolvedFalse();

    List<Escalation> findTop10ByResolvedFalseOrderByCreatedAtDesc();

    Page<Escalation> findByResolvedFalse(Pageable pageable);

    Page<Escalation> findByResolvedTrue(Pageable pageable);

    @NonNull
    Page<Escalation> findAll(@NonNull Pageable pageable);

    Optional<Escalation> findByTicketId(Long ticketId);
}
