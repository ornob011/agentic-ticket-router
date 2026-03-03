package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum LandingPage {
    DASHBOARD("Dashboard", "Show the dashboard overview"),
    TICKETS("My Tickets", "Show your tickets list"),
    QUEUE("Queue Inbox", "Show the agent queue inbox"),


    ;

    private final String displayName;
    private final String description;

    LandingPage(
        String displayName,
        String description
    ) {
        this.displayName = displayName;
        this.description = description;
    }
}
