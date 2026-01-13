package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum TicketQueue {
    BILLING_Q("Billing Queue"),
    TECH_Q("Technical Support Queue"),
    OPS_Q("Operations Queue"),
    SECURITY_Q("Security Queue"),
    ACCOUNT_Q("Account Management Queue"),
    GENERAL_Q("General Support Queue");

    private static final Set<TicketQueue> SECURITY_SENSITIVE =
            EnumSet.of(
                    SECURITY_Q
            );

    private static final Set<TicketQueue> SPECIALIZED =
            EnumSet.of(
                    BILLING_Q,
                    TECH_Q,
                    OPS_Q,
                    SECURITY_Q,
                    ACCOUNT_Q
            );

    private static final TicketQueue DEFAULT_QUEUE = GENERAL_Q;

    private final String displayName;

    TicketQueue(String displayName) {
        this.displayName = displayName;
    }

    public static TicketQueue defaultQueue() {
        return DEFAULT_QUEUE;
    }

    public boolean isSecuritySensitive() {
        return SECURITY_SENSITIVE.contains(this);
    }

    public boolean isSpecialized() {
        return SPECIALIZED.contains(this);
    }

    public boolean isGeneral() {
        return this == GENERAL_Q;
    }
}
