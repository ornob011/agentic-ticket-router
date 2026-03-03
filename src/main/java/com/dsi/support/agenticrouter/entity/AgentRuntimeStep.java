package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.AgentRuntimeStepType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "agent_runtime_step",
    indexes = {
        @Index(
            name = "idx_agent_runtime_step_run_id",
            columnList = "run_id"
        ),
        @Index(
            name = "idx_agent_runtime_step_step_no",
            columnList = "step_no"
        ),
        @Index(
            name = "idx_agent_runtime_step_step_type",
            columnList = "step_type"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRuntimeStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "run_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_agent_runtime_step_run")
    )
    private AgentRuntimeRun run;

    @Column(
        name = "step_no",
        nullable = false
    )
    private int stepNo;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "step_type",
        nullable = false,
        length = 30
    )
    private AgentRuntimeStepType stepType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "planner_output",
        columnDefinition = "jsonb"
    )
    private JsonNode plannerOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "validated_response",
        columnDefinition = "jsonb"
    )
    private JsonNode validatedResponse;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "safety_decision",
        columnDefinition = "jsonb"
    )
    private JsonNode safetyDecision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "tool_result",
        columnDefinition = "jsonb"
    )
    private JsonNode toolResult;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(
        name = "success",
        nullable = false
    )
    @Builder.Default
    private boolean success = true;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(
        name = "error_message",
        columnDefinition = "text"
    )
    private String errorMessage;
}
