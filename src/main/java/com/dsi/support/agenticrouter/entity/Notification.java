package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "notification",
    indexes = {
        @Index(
            name = "idx_notification_recipient_id",
            columnList = "recipient_id"
        ),
        @Index(
            name = "idx_notification_read",
            columnList = "read"
        ),
        @Index(
            name = "idx_notification_created_at",
            columnList = "created_at"
        ),
        @Index(
            name = "idx_notification_recipient_read",
            columnList = "recipient_id, read, created_at"
        ),
        @Index(
            name = "idx_notification_ticket_id",
            columnList = "ticket_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @NotNull(message = "Recipient is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "recipient_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_notification_recipient")
    )
    private AppUser recipient;

    @NotNull(message = "Notification type is required")
    @Enumerated(EnumType.STRING)
    @Column(
        name = "notification_type",
        nullable = false
    )
    private NotificationType notificationType;

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    @Column(
        name = "title",
        nullable = false
    )
    private String title;

    @NotBlank(message = "Body is required")
    @Column(
        name = "body",
        nullable = false,
        columnDefinition = "text"
    )
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ticket_id",
        foreignKey = @ForeignKey(name = "fk_notification_ticket")
    )
    private SupportTicket ticket;

    @Size(max = 500)
    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(
        name = "read_at",
        columnDefinition = "timestamptz"
    )
    private Instant readAt;

    public void markAsRead() {
        if (read) {
            return;
        }

        read = true;
        readAt = Instant.now();
    }

    public boolean isUrgent() {
        return Objects.nonNull(notificationType) && notificationType.isUrgent();
    }

    @Override
    public String toString() {
        return "Notification{" +
               "id=" + getId() +
               ", recipientId=" + (recipient != null ? recipient.getId() : null) +
               ", notificationType=" + notificationType +
               ", read=" + read +
               ", createdAt=" + getCreatedAt() +
               '}';
    }
}
