package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

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

    @Id
    @Column(
        name = "code",
        nullable = false,
        length = 10,
        updatable = false
    )
    @NotBlank
    @Size(
        min = 2,
        max = 10
    )
    @Pattern(
        regexp = "^[a-z]{2}(-[A-Z]{2})?$",
        message = "code must be like 'en', 'bn', or 'en-GB'"
    )
    private String code;

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

    @Override
    public String toString() {
        return "Language{" +
               "code='" + code + '\'' +
               ", name='" + name + '\'' +
               '}';
    }
}
