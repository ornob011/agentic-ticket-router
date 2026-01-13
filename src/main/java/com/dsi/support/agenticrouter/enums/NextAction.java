package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum NextAction {
    AUTO_REPLY("Generate and send automated reply"),
    ASK_CLARIFYING("Request clarifying information from customer"),
    ASSIGN_QUEUE("Assign to appropriate queue for agent handling"),
    ESCALATE("Escalate to supervisor or specialized team"),
    HUMAN_REVIEW("Requires human review before processing");

    private static final Set<NextAction> HUMAN_INTERVENTION_REQUIRED =
            EnumSet.of(
                    HUMAN_REVIEW,
                    ESCALATE
            );

    private final String description;

    NextAction(String description) {
        this.description = description;
    }

    public boolean requiresHumanIntervention() {
        return HUMAN_INTERVENTION_REQUIRED.contains(this);
    }
}
