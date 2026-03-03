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
    HUMAN_REVIEW("Requires human review before processing"),
    UPDATE_CUSTOMER_PROFILE("Update customer profile information"),
    CHANGE_PRIORITY("Change ticket priority automatically"),
    ADD_INTERNAL_NOTE("Add internal note for agents"),
    AUTO_ESCALATE("Auto-escalate to supervisor"),
    AUTO_RESOLVE("Auto-resolve ticket with solution"),
    REOPEN_TICKET("Reopen closed/resolved ticket"),
    TRIGGER_NOTIFICATION("Send specific notification"),
    USE_KNOWLEDGE_ARTICLE("Use knowledge base article for resolution"),
    USE_TEMPLATE("Use response template"),


    ;

    private static final Set<NextAction> HUMAN_INTERVENTION_REQUIRED =
        EnumSet.of(
            HUMAN_REVIEW,
            ESCALATE,
            AUTO_ESCALATE
        );

    private final String description;

    NextAction(String description) {
        this.description = description;
    }

    public boolean requiresHumanIntervention() {
        return HUMAN_INTERVENTION_REQUIRED.contains(this);
    }
}
