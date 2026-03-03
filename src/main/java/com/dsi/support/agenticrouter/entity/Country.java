package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(
    name = "country",
    indexes = {
        @Index(
            name = "idx_country_name",
            columnList = "name"
        ),
        @Index(
            name = "idx_country_active",
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

    @Id
    @NotBlank
    @Column(
        name = "iso2",
        nullable = false,
        length = 2,
        updatable = false
    )
    @Size(
        min = 2,
        max = 2
    )
    @Pattern(
        regexp = "^[A-Za-z]{2}$",
        message = "iso2 must be exactly 2 letters"
    )
    private String iso2;

    @NotBlank
    @Column(
        name = "name",
        nullable = false,
        length = 100
    )
    @Size(
        max = 100
    )
    private String name;

    @Column(
        name = "active",
        nullable = false
    )
    @Builder.Default
    private boolean active = true;

    @Override
    public String toString() {
        return "Country{" +
               "iso2='" + iso2 + '\'' +
               ", name='" + name + '\'' +
               ", active=" + active +
               '}';
    }
}
