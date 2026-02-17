package com.dsi.support.agenticrouter.service.action.handlers.profile;

public record CustomerProfileFieldChange(
    String field,
    String previousValue,
    String currentValue
) {
}
