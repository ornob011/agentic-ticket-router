package com.dsi.support.agenticrouter.service.action.actions.profile;

public record CustomerProfileUpdateOutcome(
    boolean changed,
    String changeSummary,
    String noChangeDetails
) {
}
