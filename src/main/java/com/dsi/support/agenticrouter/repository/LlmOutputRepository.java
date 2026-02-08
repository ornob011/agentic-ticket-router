package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.LlmOutput;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LlmOutputRepository extends JpaRepository<LlmOutput, Long> {

    List<LlmOutput> findByTicketIdAndOutputType(Long ticketId, LlmOutputType outputType);

    List<LlmOutput> findByOutputType(LlmOutputType outputType);

    Optional<LlmOutput> findFirstByTicketIdAndOutputTypeOrderByCreatedAtDesc(Long ticketId, LlmOutputType outputType);

    long countByOutputType(LlmOutputType outputType);

    long countByOutputTypeAndParseStatus(LlmOutputType outputType, ParseStatus parseStatus);

    @Query("""
        SELECT AVG(llmOutput.latencyMs)
        FROM LlmOutput llmOutput
        WHERE llmOutput.outputType = :outputType
        AND llmOutput.latencyMs IS NOT NULL
        """)
    Long findAverageLatencyByOutputType(@Param("outputType") LlmOutputType outputType);
}
