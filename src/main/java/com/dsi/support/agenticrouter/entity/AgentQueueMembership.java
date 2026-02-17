package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.TicketQueue;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(
    name = "agent_queue_membership",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_agent_queue_membership_user_queue",
            columnNames = {"user_id", "queue"}
        )
    },
    indexes = {
        @Index(
            name = "idx_agent_queue_membership_user_id",
            columnList = "user_id"
        ),
        @Index(
            name = "idx_agent_queue_membership_queue",
            columnList = "queue"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentQueueMembership extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_agent_queue_membership_user")
    )
    private AppUser user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
        name = "queue",
        nullable = false
    )
    private TicketQueue queue;

    @Override
    public String toString() {
        return "AgentQueueMembership{" +
               "id=" + getId() +
               ", user=" + (user == null ? "null" : user.getId()) +
               ", queue=" + (queue == null ? "null" : queue.name()) +
               '}';
    }
}
