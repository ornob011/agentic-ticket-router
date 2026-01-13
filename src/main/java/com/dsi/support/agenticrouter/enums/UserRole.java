package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum UserRole {
    CUSTOMER("Customer user with ticket submission rights"),
    AGENT("Support agent with queue access"),
    SUPERVISOR("Supervisor with escalation handling rights"),
    ADMIN("System administrator with full access");

    private static final Set<UserRole> AGENT_PORTAL_ACCESS =
            EnumSet.of(
                    AGENT,
                    SUPERVISOR,
                    ADMIN
            );

    private static final Set<UserRole> ROUTING_OVERRIDE =
            EnumSet.of(
                    SUPERVISOR,
                    ADMIN
            );

    private static final Set<UserRole> POLICY_MANAGEMENT =
            EnumSet.of(
                    ADMIN
            );

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public boolean canAccessAgentPortal() {
        return AGENT_PORTAL_ACCESS.contains(this);
    }

    public boolean canOverrideRouting() {
        return ROUTING_OVERRIDE.contains(this);
    }

    public boolean canManagePolicies() {
        return POLICY_MANAGEMENT.contains(this);
    }
}
