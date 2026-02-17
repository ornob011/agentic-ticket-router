package com.dsi.support.agenticrouter.service.action.handlers.profile;

import java.util.List;

public record CustomerProfileUpdateOutcome(
    boolean changed,
    String changeSummary,
    String noChangeDetails,
    List<CustomerProfileFieldChange> fieldChanges
) {
}
