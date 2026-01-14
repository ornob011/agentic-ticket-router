package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.ConfigValueType;
import com.dsi.support.agenticrouter.validation.ValidPolicyConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(
    name = "policy_config",
    indexes = {
        @Index(
            name = "idx_policy_config_active",
            columnList = "active"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidPolicyConfig
public class PolicyConfig extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(
        name = "config_key",
        nullable = false,
        unique = true,
        length = 100
    )
    private String configKey;

    @NotBlank
    @Size(max = 500)
    @Column(
        name = "config_value",
        nullable = false,
        length = 500
    )
    private String configValue;

    @NotNull
    @Enumerated(
        EnumType.STRING
    )
    @JdbcType(
        PostgreSQLEnumJdbcType.class
    )
    @Column(
        name = "value_type",
        nullable = false,
        columnDefinition = "config_value_type"
    )
    private ConfigValueType valueType;

    @Column(
        name = "description",
        columnDefinition = "text"
    )
    private String description;

    @Builder.Default
    @Column(
        name = "active",
        nullable = false
    )
    private boolean active = true;

    @Size(max = 500)
    @Column(
        name = "default_value",
        length = 500
    )
    private String defaultValue;

    @Column(
        name = "min_value",
        length = 50
    )
    private String minValue;

    @Column(
        name = "max_value",
        length = 50
    )
    private String maxValue;

    @Override
    public String toString() {
        return "PolicyConfig{" +
               "id=" + getId() +
               ", configKey='" + configKey + '\'' +
               ", configValue='" + configValue + '\'' +
               ", valueType=" + valueType +
               ", description='" + description + '\'' +
               ", active=" + active +
               ", defaultValue='" + defaultValue + '\'' +
               ", minValue='" + minValue + '\'' +
               ", maxValue='" + maxValue + '\'' +
               '}';
    }
}
