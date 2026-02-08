package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

@Entity
@Table(
    name = "llm_output",
    indexes = {
        @Index(
            name = "idx_llm_output_ticket_id",
            columnList = "ticket_id"
        ),
        @Index(
            name = "idx_llm_output_created_at",
            columnList = "created_at"
        ),
        @Index(
            name = "idx_llm_output_parse_status",
            columnList = "parse_status"
        ),
        @Index(
            name = "idx_llm_output_model_tag",
            columnList = "model_tag"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmOutput extends BaseEntity {

    @NotNull(message = "Ticket is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ticket_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_llm_output_ticket")
    )
    private SupportTicket ticket;

    @NotNull(message = "Model tag is required")
    @Size(max = 100)
    @Column(
        name = "model_tag",
        nullable = false,
        length = 100
    )
    private String modelTag;

    @NotNull(message = "Output type is required")
    @Enumerated(EnumType.STRING)
    @Column(
        name = "output_type",
        nullable = false
    )
    private LlmOutputType outputType;

    @NotNull(message = "Raw request is required")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "raw_request",
        nullable = false,
        columnDefinition = "jsonb"
    )
    private JsonNode rawRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "raw_response",
        columnDefinition = "jsonb"
    )
    private JsonNode rawResponse;

    @NotNull(message = "Parse status is required")
    @Enumerated(EnumType.STRING)
    @Column(
        name = "parse_status",
        nullable = false
    )
    private ParseStatus parseStatus;

    @Column(
        name = "error_message",
        columnDefinition = "text"
    )
    private String errorMessage;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "repair_attempts", nullable = false)
    @Builder.Default
    private int repairAttempts = 0;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "inference_config",
        columnDefinition = "jsonb"
    )
    private JsonNode inferenceConfig;

    public boolean isParseSuccessful() {
        return Objects.nonNull(parseStatus) && parseStatus.isSuccess();
    }

    public boolean requiresHumanReview() {
        return Objects.nonNull(parseStatus) && parseStatus.requiresHumanReview();
    }

    public boolean canRetry() {
        return Objects.nonNull(parseStatus) && parseStatus.canRetry() && repairAttempts < 2;
    }

    public void incrementRepairAttempts() {
        repairAttempts = repairAttempts + 1;
    }

    @Override
    public String toString() {
        return "LlmOutput{" +
               "id=" + getId() +
               ", ticketId=" + (ticket != null ? ticket.getId() : null) +
               ", modelTag='" + modelTag + '\'' +
               ", parseStatus=" + parseStatus +
               ", latencyMs=" + latencyMs +
               ", repairAttempts=" + repairAttempts +
               '}';
    }
}
