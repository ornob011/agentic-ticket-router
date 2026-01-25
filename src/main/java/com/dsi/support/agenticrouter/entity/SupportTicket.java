package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
    name = "support_ticket",
    indexes = {
        @Index(
            name = "idx_support_ticket_ticket_no",
            columnList = "ticket_no",
            unique = true
        ),
        @Index(
            name = "idx_support_ticket_customer_id",
            columnList = "customer_id"
        ),
        @Index(
            name = "idx_support_ticket_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_support_ticket_assigned_queue",
            columnList = "assigned_queue"
        ),
        @Index(
            name = "idx_support_ticket_current_priority",
            columnList = "current_priority"
        ),
        @Index(
            name = "idx_support_ticket_last_activity_at",
            columnList = "last_activity_at"
        ),
        @Index(
            name = "idx_support_ticket_assigned_agent_id",
            columnList = "assigned_agent_id"
        ),
        @Index(
            name = "idx_support_ticket_status_queue_priority",
            columnList = "status, assigned_queue, current_priority"
        ),
        @Index(
            name = "idx_support_ticket_customer_created",
            columnList = "customer_id, created_at"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket extends BaseEntity {

    @Generated(event = EventType.INSERT)
    @Column(
        name = "ticket_no",
        nullable = false,
        unique = true,
        updatable = false,
        insertable = false
    )
    private Long ticketNo;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "customer_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_support_ticket_customer")
    )
    private AppUser customer;

    @NotBlank(message = "Subject is required")
    @Size(max = 255)
    @Column(
        name = "subject",
        nullable = false,
        length = 255
    )
    private String subject;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false
    )
    @Builder.Default
    private TicketStatus status = TicketStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "current_category"
    )
    private TicketCategory currentCategory;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(
        name = "current_priority",
        nullable = false
    )
    @Builder.Default
    private TicketPriority currentPriority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "assigned_queue"
    )
    private TicketQueue assignedQueue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "assigned_agent_id",
        foreignKey = @ForeignKey(name = "fk_support_ticket_assigned_agent")
    )
    private AppUser assignedAgent;

    @Column(
        name = "last_activity_at",
        nullable = false,
        columnDefinition = "timestamptz"
    )
    @Builder.Default
    private Instant lastActivityAt = Instant.now();

    @Column(
        name = "first_assigned_at",
        columnDefinition = "timestamptz"
    )
    private Instant firstAssignedAt;

    @Column(
        name = "resolved_at",
        columnDefinition = "timestamptz"
    )
    private Instant resolvedAt;

    @Column(
        name = "closed_at",
        columnDefinition = "timestamptz"
    )
    private Instant closedAt;

    @Column(
        name = "reopen_count",
        nullable = false
    )
    @Builder.Default
    private int reopenCount = 0;

    @Column(
        name = "escalated",
        nullable = false
    )
    @Builder.Default
    private boolean escalated = false;

    @Column(
        name = "latest_routing_confidence",
        columnDefinition = "numeric(5,4)"
    )
    private BigDecimal latestRoutingConfidence;

    @Column(
        name = "latest_routing_version",
        nullable = false
    )
    @Builder.Default
    private int latestRoutingVersion = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "metadata",
        columnDefinition = "jsonb"
    )
    private JsonNode metadata;

    @OneToMany(
        mappedBy = "ticket",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<TicketMessage> messages = new ArrayList<>();

    @OneToMany(
        mappedBy = "ticket",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<TicketRouting> routings = new ArrayList<>();

    @OneToOne(
        mappedBy = "ticket",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private Escalation escalation;

    @PrePersist
    public void prePersist() {
        if (Objects.isNull(lastActivityAt)) {
            lastActivityAt = Instant.now();
        }
    }

    public String getFormattedTicketNo() {
        return Objects.nonNull(ticketNo)
            ? String.format("TKT-%08d", ticketNo)
            : "PENDING";
    }

    public boolean requiresCustomerAction() {
        return Objects.nonNull(status) && status.requiresCustomerAction();
    }

    public boolean requiresAgentAction() {
        return Objects.nonNull(status) && status.requiresAgentAction();
    }

    public boolean isTerminal() {
        return Objects.nonNull(status) && status.isTerminalState();
    }

    public void updateLastActivity() {
        lastActivityAt = Instant.now();
    }

    public void incrementReopenCount() {
        reopenCount = reopenCount + 1;
    }

    public void addMessage(TicketMessage message) {
        if (Objects.isNull(message)) {
            return;
        }
        message.setTicket(this);
        messages.add(message);
    }

    public void addRouting(TicketRouting routing) {
        if (Objects.isNull(routing)) {
            return;
        }
        routing.setTicket(this);
        routings.add(routing);
    }

    @Override
    public String toString() {
        return "SupportTicket{" +
               "id=" + getId() +
               ", ticketNo=" + ticketNo +
               ", subject='" + subject + '\'' +
               ", status=" + status +
               ", currentPriority=" + currentPriority +
               ", assignedQueue=" + assignedQueue +
               '}';
    }
}
