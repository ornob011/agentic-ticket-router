package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.LandingPage;
import com.dsi.support.agenticrouter.enums.ThemePreference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(
    name = "user_settings",
    indexes = {
        @Index(
            name = "idx_user_settings_user_id",
            columnList = "user_id",
            unique = true
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @NotNull(message = "User is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        unique = true,
        foreignKey = @ForeignKey(name = "fk_user_settings_user")
    )
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_landing", nullable = false)
    @Builder.Default
    private LandingPage defaultLanding = LandingPage.DASHBOARD;

    @Column(name = "sidebar_collapsed", nullable = false)
    @Builder.Default
    private boolean sidebarCollapsed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false)
    @Builder.Default
    private ThemePreference theme = ThemePreference.SYSTEM;

    @Column(name = "compact_mode", nullable = false)
    @Builder.Default
    private boolean compactMode = false;

    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean emailNotificationsEnabled = true;

    @Column(name = "notify_ticket_reply", nullable = false)
    @Builder.Default
    private boolean notifyTicketReply = true;

    @Column(name = "notify_status_change", nullable = false)
    @Builder.Default
    private boolean notifyStatusChange = true;

    @Column(name = "notify_escalation", nullable = false)
    @Builder.Default
    private boolean notifyEscalation = true;

    @Override
    public String toString() {
        return "UserSettings{" +
               "id=" + getId() +
               ", userId=" + (user != null ? user.getId() : null) +
               ", defaultLanding=" + defaultLanding +
               ", theme=" + theme +
               '}';
    }
}
