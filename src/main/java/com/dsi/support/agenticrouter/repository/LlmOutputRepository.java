package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.LlmOutput;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmOutputRepository extends JpaRepository<LlmOutput, Long> {
}
