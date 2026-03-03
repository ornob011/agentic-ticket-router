package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.LlmOutput;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LlmOutputRepository extends JpaRepository<LlmOutput, Long> {

    @Query("""
        SELECT AVG(llmOutput.latencyMs)
        FROM LlmOutput llmOutput
        WHERE llmOutput.outputType = :outputType
        AND llmOutput.latencyMs IS NOT NULL
        """)
    Long findAverageLatencyByOutputType(@Param("outputType") LlmOutputType outputType);
}
