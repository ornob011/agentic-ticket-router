package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "ref_country",
        indexes = {
                @Index(
                        name = "idx_ref_country_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_ref_country_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {

    /**
     * ISO 3166-1 alpha-2 (e.g. "BD", "US")
     */
    @Id
    @Column(
            name = "iso2",
            nullable = false,
            length = 2,
            updatable = false
    )
    @Size(min = 2, max = 2)
    private String iso2;

    @NotBlank
    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    @Size(max = 100)
    private String name;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (Objects.nonNull(iso2)) {
            iso2 = iso2.trim().toUpperCase();
        }

        if (Objects.nonNull(name)) {
            name = name.trim();
        }
    }
}
