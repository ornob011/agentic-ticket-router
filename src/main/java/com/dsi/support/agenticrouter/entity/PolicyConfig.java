package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.ConfigValueType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
        name = "policy_config",
        indexes = {
                @Index(
                        name = "idx_policy_config_config_key",
                        columnList = "config_key",
                        unique = true
                ),
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
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(
            name = "value_type",
            nullable = false,
            columnDefinition = "config_value_type"
    )
    private ConfigValueType valueType;

    @Size(max = 500)
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

    public String effectiveValue() {
        return Objects.nonNull(configValue)
                ? configValue
                : defaultValue;
    }

    public BigDecimal getValueAsDecimal() {
        String value = effectiveValue();

        if (Objects.isNull(value)) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    public Integer getValueAsInteger() {
        BigDecimal value = getValueAsDecimal();
        return Objects.nonNull(value)
                ? value.intValue()
                : null;
    }

    public Long getValueAsLong() {
        BigDecimal value = getValueAsDecimal();
        return Objects.nonNull(value)
                ? value.longValue()
                : null;
    }

    public Boolean getValueAsBoolean() {
        String value = effectiveValue();

        return Objects.nonNull(value)
                ? Boolean.parseBoolean(value)
                : null;
    }

    public boolean isValueValid(String value) {
        if (Objects.isNull(valueType) || Objects.isNull(value)) {
            return false;
        }

        try {
            if (valueType.isNumeric()) {
                BigDecimal numeric = new BigDecimal(value);

                if (Objects.nonNull(minValue)
                        && numeric.compareTo(new BigDecimal(minValue)) < 0) {
                    return false;
                }

                if (Objects.nonNull(maxValue)
                        && numeric.compareTo(new BigDecimal(maxValue)) > 0) {
                    return false;
                }

                return true;
            }

            if (Objects.equals(valueType, ConfigValueType.BOOLEAN)) {
                return "true".equalsIgnoreCase(value)
                        || "false".equalsIgnoreCase(value);
            }

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "PolicyConfig{" +
                "id=" + getId() +
                ", configKey='" + configKey + '\'' +
                ", valueType=" + valueType +
                ", active=" + active +
                '}';
    }
}
