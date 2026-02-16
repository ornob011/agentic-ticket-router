package com.dsi.support.agenticrouter.service.action.actions.profile;

public record CustomerProfileFieldChange(
    String field,
    String previousValue,
    String currentValue
) {
}
