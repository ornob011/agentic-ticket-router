package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum TicketStatus {
    RECEIVED("Received", "Ticket received, pending initial processing"),
    TRIAGING("Triaging", "AI router analyzing ticket"),
    WAITING_CUSTOMER("Waiting for Customer", "Awaiting customer response"),
    ASSIGNED("Assigned", "Assigned to queue, awaiting agent pickup"),
    IN_PROGRESS("In Progress", "Agent actively working on ticket"),
    RESOLVED("Resolved", "Issue resolved, awaiting customer confirmation"),
    ESCALATED("Escalated", "Escalated to higher tier support"),
    AUTO_ESCALATED("Auto Escalated", "Autonomous processing exhausted - requires human review"),
    AUTO_CLOSED_PENDING("Auto Close Pending", "Pending auto-closure due to inactivity"),
    CLOSED("Closed", "Ticket closed"),


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

    private static final Set<TicketStatus> QUEUE_INBOX_DEFAULTS =
        EnumSet.of(
            RECEIVED,
            TRIAGING,
            WAITING_CUSTOMER,
            ASSIGNED,
            IN_PROGRESS,
            ESCALATED
        );

    private static final Set<TicketStatus> REQUIRES_CATEGORY_MISMATCH_CHECK =
        EnumSet.of(
            CLOSED,
            AUTO_CLOSED_PENDING,
            AUTO_ESCALATED
        );

    private final String displayName;
    private final String description;

    TicketStatus(
        String displayName,
        String description
    ) {
        this.displayName = displayName;
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

    public boolean isClosedForReplies() {
        return REQUIRES_CATEGORY_MISMATCH_CHECK.contains(this);
    }

    public static Set<TicketStatus> queueInboxDefaults() {
        return QUEUE_INBOX_DEFAULTS;
    }
}
