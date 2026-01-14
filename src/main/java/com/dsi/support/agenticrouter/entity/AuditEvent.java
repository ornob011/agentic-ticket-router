package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

@Entity
@Table(
    name = "audit_event",
    indexes = {
        @Index(
            name = "idx_audit_event_ticket_id",
            columnList = "ticket_id"
        ),
        @Index(
            name = "idx_audit_event_event_type",
            columnList = "event_type"
        ),
        @Index(
            name = "idx_audit_event_performed_by_id",
            columnList = "performed_by_id"
        ),
        @Index(
            name = "idx_audit_event_created_at",
            columnList = "created_at"
        ),
        @Index(
            name = "idx_audit_event_ticket_created",
            columnList = "ticket_id, created_at"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ticket_id",
        foreignKey = @ForeignKey(name = "fk_audit_event_ticket")
    )
    private SupportTicket ticket;

    @NotNull(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
        name = "event_type",
        nullable = false,
        columnDefinition = "audit_event_type"
    )
    private AuditEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "performed_by_id",
        foreignKey = @ForeignKey(name = "fk_audit_event_performed_by")
    )
    private AppUser performedBy;

    @NotBlank(message = "Description is required")
    @Column(
        name = "description",
        nullable = false,
        columnDefinition = "text"
    )
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "payload",
        columnDefinition = "jsonb"
    )
    private JsonNode payload;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    public boolean isSystemGenerated() {
        return Objects.isNull(performedBy) ||
               (Objects.nonNull(eventType) && eventType.isSystemGenerated());
    }

    public boolean isError() {
        return Objects.nonNull(eventType) && eventType.isErrorEvent();
    }

    @Override
    public String toString() {
        return "AuditEvent{" +
               "id=" + getId() +
               ", ticketId=" + (ticket != null ? ticket.getId() : null) +
               ", eventType=" + eventType +
               ", performedBy=" + (performedBy != null ? performedBy.getUsername() : "SYSTEM") +
               ", createdAt=" + getCreatedAt() +
               '}';
    }
}
