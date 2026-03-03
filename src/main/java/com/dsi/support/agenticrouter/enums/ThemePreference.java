package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum ThemePreference {
    LIGHT("Light", "Light theme"),
    DARK("Dark", "Dark theme"),
    SYSTEM("System", "Follow system preference"),


    ;

    private final String displayName;
    private final String description;

    ThemePreference(
        String displayName,
        String description
    ) {
        this.displayName = displayName;
        this.description = description;
    }
}
