package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.validator.ValidPolicyConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

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

    @NotNull
    @Enumerated(
        EnumType.STRING
    )
    @Column(
        name = "config_key",
        nullable = false,
        unique = true
    )
    private PolicyConfigKey configKey;

    @Column(
        name = "config_value",
        nullable = false
    )
    private BigDecimal configValue;

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

    @Column(
        name = "default_value"
    )
    private BigDecimal defaultValue;

    @Column(
        name = "min_value"
    )
    private BigDecimal minValue;

    @Column(
        name = "max_value"
    )
    private BigDecimal maxValue;

    @Override
    public String toString() {
        return "PolicyConfig{" +
               "id=" + getId() +
               ", configKey=" + configKey +
               ", configValue=" + configValue +
               ", description='" + description + '\'' +
               ", active=" + active +
               ", defaultValue=" + defaultValue +
               ", minValue=" + minValue +
               ", maxValue=" + maxValue +
               '}';
    }
}
