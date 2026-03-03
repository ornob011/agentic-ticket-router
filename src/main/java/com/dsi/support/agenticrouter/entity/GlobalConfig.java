package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.GlobalConfigKey;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Objects;

@Entity
@Table(
    name = "global_config",
    indexes = {
        @Index(
            name = "idx_global_config_key_active",
            columnList = "config_key"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(
        name = "config_key",
        nullable = false,
        unique = true
    )
    private GlobalConfigKey configKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "map_value",
        nullable = false,
        columnDefinition = "jsonb"
    )
    @Builder.Default
    private HashMap<String, Object> mapValue = new HashMap<>();

    public <T> T getValue(
        String key,
        Class<T> type
    ) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");

        if (mapValue == null) {
            return null;
        }

        Object value = mapValue.get(key);

        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        throw new ClassCastException(
            String.format(
                "Config value for key '%s' is of type %s, cannot cast to %s",
                key,
                value.getClass().getName(),
                type.getName()
            )
        );
    }

    public void setValue(
        String key,
        Object value
    ) {
        Objects.requireNonNull(key, "key");

        mapValue = Objects.requireNonNullElseGet(mapValue, HashMap::new);

        if (value == null) {
            mapValue.remove(key);
            return;
        }

        mapValue.put(key, value);
    }

    @Override
    public String toString() {
        return "GlobalConfig{" +
               "id=" + getId() +
               ", configKey=" + configKey +
               ", mapValue=" + mapValue +
               '}';
    }
}
