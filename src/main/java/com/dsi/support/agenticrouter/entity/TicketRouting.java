package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "ticket_routing",
        indexes = {
                @Index(
                        name = "idx_ticket_routing_ticket_id",
                        columnList = "ticket_id"
                ),
                @Index(
                        name = "idx_ticket_routing_ticket_version",
                        columnList = "ticket_id, version",
                        unique = true
                ),
                @Index(
                        name = "idx_ticket_routing_llm_output_id",
                        columnList = "llm_output_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRouting extends BaseEntity {

    @NotNull(message = "Ticket is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_routing_ticket")
    )
    private SupportTicket ticket;

    @NotNull(message = "Version is required")
    @Column(name = "version", nullable = false)
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "llm_output_id",
            foreignKey = @ForeignKey(name = "fk_ticket_routing_llm_output")
    )
    private LlmOutput llmOutput;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
            name = "category",
            nullable = false,
            columnDefinition = "ticket_category"
    )
    private TicketCategory category;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
            name = "priority",
            nullable = false,
            columnDefinition = "ticket_priority"
    )
    private TicketPriority priority;

    @NotNull(message = "Queue is required")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
            name = "queue",
            nullable = false,
            columnDefinition = "ticket_queue"
    )
    private TicketQueue queue;

    @NotNull(message = "Next action is required")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
            name = "next_action",
            nullable = false,
            columnDefinition = "next_action"
    )
    private NextAction nextAction;

    @NotNull(message = "Confidence is required")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Column(
            name = "confidence",
            nullable = false,
            precision = 5,
            scale = 4
    )
    private Double confidence;

    @Column(
            name = "clarifying_question",
            columnDefinition = "text"
    )
    private String clarifyingQuestion;

    @Column(
            name = "draft_reply",
            columnDefinition = "text"
    )
    private String draftReply;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "rationale_tags",
            columnDefinition = "jsonb"
    )
    @Builder.Default
    private List<String> rationaleTags = new ArrayList<>();

    @Column(name = "overridden", nullable = false)
    @Builder.Default
    private boolean overridden = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "overridden_by_id",
            foreignKey = @ForeignKey(name = "fk_ticket_routing_overridden_by")
    )
    private AppUser overriddenBy;

    @Column(
            name = "override_reason",
            columnDefinition = "text"
    )
    private String overrideReason;

    @Column(name = "policy_gate_triggered", nullable = false)
    @Builder.Default
    private boolean policyGateTriggered = false;

    @Column(name = "triggered_policy_name", length = 100)
    private String triggeredPolicyName;

    @Column(name = "applied", nullable = false)
    @Builder.Default
    private boolean applied = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "metadata",
            columnDefinition = "jsonb"
    )
    private JsonNode metadata;

    public boolean meetsAutoRoutingThreshold(double threshold) {
        return Objects.nonNull(confidence) && confidence >= threshold;
    }

    public boolean requiresHumanIntervention() {
        return Objects.nonNull(nextAction) && nextAction.requiresHumanIntervention();
    }

    @Override
    public String toString() {
        return "TicketRouting{" +
                "id=" + getId() +
                ", ticketId=" + (ticket != null ? ticket.getId() : null) +
                ", version=" + version +
                ", category=" + category +
                ", priority=" + priority +
                ", queue=" + queue +
                ", nextAction=" + nextAction +
                ", confidence=" + confidence +
                ", overridden=" + overridden +
                ", applied=" + applied +
                '}';
    }
}
