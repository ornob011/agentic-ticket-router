package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "app_user",
    indexes = {
        @Index(
            name = "idx_app_user_email",
            columnList = "email",
            unique = true
        ),
        @Index(
            name = "idx_app_user_username",
            columnList = "username",
            unique = true
        ),
        @Index(
            name = "idx_app_user_role",
            columnList = "role"
        ),
        @Index(
            name = "idx_app_user_active_role",
            columnList = "active, role"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser extends BaseEntity {

    @NotBlank(message = "Username is required")
    @Size(max = 50)
    @Column(
        name = "username",
        nullable = false,
        unique = true,
        length = 50
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100)
    @Column(
        name = "email",
        nullable = false,
        unique = true,
        length = 100
    )
    private String email;

    @NotBlank(message = "Password hash is required")
    @Size(max = 512)
    @Column(
        name = "password_hash",
        nullable = false
    )
    private String passwordHash;

    @Size(max = 100)
    @Column(
        name = "full_name",
        length = 100
    )
    private String fullName;

    @NotNull(message = "User role is required")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
        name = "role",
        nullable = false,
        columnDefinition = "user_role"
    )
    private UserRole role;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(
        name = "last_login_at",
        columnDefinition = "timestamptz"
    )
    private Instant lastLoginAt;

    @Size(max = 45)
    @Column(
        name = "last_login_ip",
        length = 45
    )
    private String lastLoginIp;

    @OneToOne(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private CustomerProfile customerProfile;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (Objects.nonNull(email)) {
            email = email.trim().toLowerCase();
        }

        if (Objects.nonNull(username)) {
            username = username.trim().toLowerCase();
        }

        if (Objects.nonNull(fullName)) {
            fullName = fullName.trim();
        }
    }

    public boolean isCustomer() {
        return Objects.equals(role, UserRole.CUSTOMER);
    }

    public boolean isAgent() {
        return Objects.equals(role, UserRole.AGENT);
    }

    public boolean isSupervisor() {
        return Objects.equals(role, UserRole.SUPERVISOR);
    }

    public boolean isAdmin() {
        return Objects.equals(role, UserRole.ADMIN);
    }

    public boolean canAccessAgentPortal() {
        return Objects.nonNull(role) && role.canAccessAgentPortal();
    }

    public boolean canOverrideRouting() {
        return Objects.nonNull(role) && role.canOverrideRouting();
    }

    @Override
    public String toString() {
        return "AppUser{" +
               "id=" + getId() +
               ", username='" + username + '\'' +
               ", email='" + email + '\'' +
               ", role=" + role +
               ", active=" + active +
               '}';
    }
}
