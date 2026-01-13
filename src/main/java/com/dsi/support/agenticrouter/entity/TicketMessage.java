package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.MessageKind;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.util.Objects;

@Entity
@Table(
        name = "ticket_message",
        indexes = {
                @Index(
                        name = "idx_ticket_message_ticket_id",
                        columnList = "ticket_id"
                ),
                @Index(
                        name = "idx_ticket_message_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_ticket_message_ticket_created",
                        columnList = "ticket_id, created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessage extends BaseEntity {

    @NotNull(message = "Ticket is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_message_ticket")
    )
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "author_id",
            foreignKey = @ForeignKey(name = "fk_ticket_message_author")
    )
    private AppUser author;

    @NotNull(message = "Message kind is required")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
            name = "message_kind",
            nullable = false,
            columnDefinition = "message_kind"
    )
    private MessageKind messageKind;

    @NotBlank(message = "Content is required")
    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "text"
    )
    private String content;

    @Column(name = "visible_to_customer", nullable = false)
    @Builder.Default
    private boolean visibleToCustomer = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "llm_output_id",
            foreignKey = @ForeignKey(name = "fk_ticket_message_llm_output")
    )
    private LlmOutput llmOutput;

    @Column(name = "read_by_customer", nullable = false)
    @Builder.Default
    private boolean readByCustomer = false;

    @Column(name = "read_by_agent", nullable = false)
    @Builder.Default
    private boolean readByAgent = false;

    public boolean isFromCustomer() {
        return Objects.equals(messageKind, MessageKind.CUSTOMER_MESSAGE);
    }

    public boolean isFromAgent() {
        return Objects.equals(messageKind, MessageKind.AGENT_MESSAGE);
    }

    public boolean isSystemGenerated() {
        return Objects.nonNull(messageKind) && messageKind.isAutomated();
    }

    public boolean isVisibleToCustomer() {
        return visibleToCustomer && (Objects.isNull(messageKind) || messageKind.isVisibleToCustomer());
    }

    @Override
    public String toString() {
        return "TicketMessage{" +
                "id=" + getId() +
                ", ticketId=" + (ticket != null ? ticket.getId() : null) +
                ", messageKind=" + messageKind +
                ", visibleToCustomer=" + visibleToCustomer +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
