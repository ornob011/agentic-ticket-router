package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum TicketStatus {
    RECEIVED("Ticket received, pending initial processing"),
    TRIAGING("AI router analyzing ticket"),
    WAITING_CUSTOMER("Awaiting customer response"),
    ASSIGNED("Assigned to queue, awaiting agent pickup"),
    IN_PROGRESS("Agent actively working on ticket"),
    RESOLVED("Issue resolved, awaiting customer confirmation"),
    ESCALATED("Escalated to higher tier support"),
    AUTO_ESCALATED("Autonomous processing exhausted - requires human review"),
    AUTO_CLOSED_PENDING("Pending auto-closure due to inactivity"),
    CLOSED("Ticket closed"),


    ;

    private static final Set<TicketStatus> TERMINAL =
        EnumSet.of(
            CLOSED,
            AUTO_ESCALATED
        );

    private static final Set<TicketStatus> CUSTOMER_ACTION_REQUIRED =
        EnumSet.of(
            WAITING_CUSTOMER
        );

    private static final Set<TicketStatus> AGENT_ACTION_REQUIRED =
        EnumSet.of(
            ASSIGNED,
            IN_PROGRESS,
            ESCALATED
        );

    private static final Set<TicketStatus> ACTIVE_WORK =
        EnumSet.of(
            ASSIGNED,
            IN_PROGRESS,
            ESCALATED,
            WAITING_CUSTOMER,
            TRIAGING
        );

    private final String description;

    TicketStatus(String description) {
        this.description = description;
    }

    public boolean isTerminalState() {
        return TERMINAL.contains(this);
    }

    public boolean requiresCustomerAction() {
        return CUSTOMER_ACTION_REQUIRED.contains(this);
    }

    public boolean requiresAgentAction() {
        return AGENT_ACTION_REQUIRED.contains(this);
    }

    public boolean isActiveWork() {
        return ACTIVE_WORK.contains(this);
    }
}
