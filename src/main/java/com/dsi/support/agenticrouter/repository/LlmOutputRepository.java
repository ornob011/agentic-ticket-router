package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.LlmOutput;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LlmOutputRepository extends JpaRepository<LlmOutput, Long> {

    List<LlmOutput> findByTicketIdAndOutputType(Long ticketId, LlmOutputType outputType);

    List<LlmOutput> findByOutputType(LlmOutputType outputType);
}
