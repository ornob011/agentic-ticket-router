package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.AgentRuntimeRunStatus;
import com.dsi.support.agenticrouter.enums.AgentTerminationReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "agent_runtime_run",
    indexes = {
        @Index(
            name = "idx_agent_runtime_run_ticket_id",
            columnList = "ticket_id"
        ),
        @Index(
            name = "idx_agent_runtime_run_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_agent_runtime_run_correlation_id",
            columnList = "correlation_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRuntimeRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ticket_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_agent_runtime_run_ticket")
    )
    private SupportTicket ticket;

    @Column(
        name = "correlation_id",
        length = 100
    )
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 30
    )
    private AgentRuntimeRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "termination_reason",
        length = 50
    )
    private AgentTerminationReason terminationReason;

    @Column(name = "total_steps")
    private Integer totalSteps;

    @Column(
        name = "fallback_used",
        nullable = false
    )
    @Builder.Default
    private boolean fallbackUsed = false;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(
        name = "error_message",
        columnDefinition = "text"
    )
    private String errorMessage;

    @Column(
        name = "started_at",
        nullable = false,
        columnDefinition = "timestamptz"
    )
    private Instant startedAt;

    @Column(
        name = "ended_at",
        columnDefinition = "timestamptz"
    )
    private Instant endedAt;
}
