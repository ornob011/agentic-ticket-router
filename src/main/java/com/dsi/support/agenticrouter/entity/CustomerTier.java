package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

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
    @NotBlank
    @Size(
        max = 50
    )
    @Pattern(
        regexp = "^[A-Z0-9_]+$",
        message = "code must be uppercase alphanumerics/underscore (e.g. FREE, STANDARD)"
    )
    private String code;

    @NotBlank
    @Column(
        name = "display_name",
        nullable = false,
        length = 100
    )
    @Size(
        max = 100
    )
    private String displayName;

    @Column(
        name = "active",
        nullable = false
    )
    @Builder.Default
    private boolean active = true;

    @Override
    public String toString() {
        return "CustomerTier{" +
               "code='" + code + '\'' +
               ", displayName='" + displayName + '\'' +
               ", active=" + active +
               '}';
    }
}
