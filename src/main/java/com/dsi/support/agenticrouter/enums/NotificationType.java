package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum NotificationType {
    TICKET_ACK("Ticket acknowledgment"),
    STATUS_CHANGE("Ticket status changed"),
    NEW_MESSAGE("New message on ticket"),
    SLA_REMINDER("SLA breach reminder"),
    ESCALATION("Ticket escalated"),
    AUTO_CLOSE_WARNING("Ticket will auto-close soon"),
    TICKET_REOPENED("Ticket reopened"),
    ASSIGNED_TO_YOU("Ticket assigned to you");

    private static final Set<NotificationType> URGENT =
        EnumSet.of(
            ESCALATION,
            SLA_REMINDER
        );

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public boolean isUrgent() {
        return URGENT.contains(this);
    }
}
