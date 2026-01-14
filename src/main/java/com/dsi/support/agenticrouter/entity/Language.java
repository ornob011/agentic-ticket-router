package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "language",
        indexes = {
                @Index(
                        name = "idx_language_name",
                        columnList = "name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Language {

    /**
     * ISO 639-1/BCP47-ish short code (e.g. "en", "bn", "en-GB")
     */
    @Id
    @Column(
            name = "code",
            nullable = false,
            length = 10,
            updatable = false
    )
    @Size(min = 2, max = 10)
    private String code;

    @NotBlank
    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    @Size(max = 100)
    private String name;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (Objects.nonNull(code)) {
            code = code.trim().toLowerCase();
        }

        if (Objects.nonNull(name)) {
            name = name.trim();
        }
    }
}
