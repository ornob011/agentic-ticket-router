package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum ConfigValueType {
    DOUBLE("Floating point number"),
    INTEGER("32-bit integer"),
    LONG("64-bit integer"),
    BOOLEAN("true/false"),
    STRING("Plain string"),
    JSON("JSON blob");

    private static final Set<ConfigValueType> NUMERIC_TYPES =
        EnumSet.of(
            DOUBLE,
            INTEGER,
            LONG
        );

    private final String description;

    ConfigValueType(String description) {
        this.description = description;
    }

    public boolean isNumeric() {
        return NUMERIC_TYPES.contains(this);
    }
}
