package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "escalation",
        indexes = {
                @Index(
                        name = "idx_escalation_ticket_id",
                        columnList = "ticket_id",
                        unique = true
                ),
                @Index(
                        name = "idx_escalation_assigned_supervisor_id",
                        columnList = "assigned_supervisor_id"
                ),
                @Index(
                        name = "idx_escalation_resolved",
                        columnList = "resolved"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Escalation extends BaseEntity {

    @NotNull(message = "Ticket is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_escalation_ticket")
    )
    private SupportTicket ticket;

    @NotBlank(message = "Escalation reason is required")
    @Column(
            name = "reason",
            nullable = false,
            columnDefinition = "text"
    )
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assigned_supervisor_id",
            foreignKey = @ForeignKey(name = "fk_escalation_supervisor")
    )
    private AppUser assignedSupervisor;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private boolean resolved = false;

    @Column(
            name = "resolved_at",
            columnDefinition = "timestamptz"
    )
    private Instant resolvedAt;

    @Column(
            name = "resolution_notes",
            columnDefinition = "text"
    )
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "resolved_by_id",
            foreignKey = @ForeignKey(name = "fk_escalation_resolved_by")
    )
    private AppUser resolvedBy;

    public void markResolved(AppUser resolver, String notes) {
        if (resolved) {
            return;
        }

        resolved = true;
        resolvedAt = Instant.now();
        resolvedBy = resolver;
        resolutionNotes = notes;
    }

    @Override
    public String toString() {
        return "Escalation{" +
                "id=" + getId() +
                ", ticketId=" + (ticket != null ? ticket.getId() : null) +
                ", resolved=" + resolved +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
}
