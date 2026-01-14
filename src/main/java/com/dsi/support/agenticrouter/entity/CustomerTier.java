package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "customer_tier",
        indexes = {
                @Index(
                        name = "idx_customer_tier_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTier {

    @Id
    @Column(
            name = "code",
            nullable = false,
            length = 50,
            updatable = false
    )
    @Size(max = 50)
    private String code;

    @NotBlank
    @Column(
            name = "display_name",
            nullable = false,
            length = 100
    )
    @Size(max = 100)
    private String displayName;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (Objects.nonNull(code)) {
            code = code.trim().toUpperCase();
        }

        if (Objects.nonNull(displayName)) {
            displayName = displayName.trim();
        }
    }
}
