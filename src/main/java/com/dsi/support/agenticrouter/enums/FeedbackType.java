package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum FeedbackType {
    RATING("Rating", "Agent rated the AI decision quality"),
    CORRECTION("Correction", "Agent corrected the AI decision"),
    REJECTION("Rejection", "Agent rejected the AI decision entirely"),
    APPROVAL("Approval", "Agent explicitly approved the AI decision"),


    ;

    private final String displayName;
    private final String description;

    FeedbackType(
        String displayName,
        String description
    ) {
        this.displayName = displayName;
        this.description = description;
    }
}
