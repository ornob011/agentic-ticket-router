package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum TicketPriority {
    CRITICAL("Critical - Immediate attention required"),
    HIGH("High - Urgent resolution needed"),
    MEDIUM("Medium - Normal priority"),
    LOW("Low - Can be addressed when time permits");

    private static final Set<TicketPriority> URGENT =
            EnumSet.of(
                    CRITICAL,
                    HIGH
            );

    private static final Set<TicketPriority> NON_URGENT =
            EnumSet.complementOf(
                    EnumSet.of(
                            CRITICAL,
                            HIGH
                    )
            );

    private final String description;

    TicketPriority(String description) {
        this.description = description;
    }

    public boolean isUrgent() {
        return URGENT.contains(this);
    }

    public boolean isNonUrgent() {
        return NON_URGENT.contains(this);
    }
}
