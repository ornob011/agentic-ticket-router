package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum MessageKind {
    CUSTOMER_MESSAGE("Message from customer"),
    AGENT_MESSAGE("Message from support agent"),
    SYSTEM_MESSAGE("Automated system message"),
    CLARIFYING_QUESTION("AI-generated clarifying question"),
    AUTO_REPLY("AI-generated automated reply"),
    INTERNAL_NOTE("Internal note (not visible to customer)");

    private static final Set<MessageKind> VISIBLE_TO_CUSTOMER =
            EnumSet.complementOf(
                    EnumSet.of(
                            INTERNAL_NOTE
                    )
            );

    private static final Set<MessageKind> AUTOMATED =
            EnumSet.of(
                    SYSTEM_MESSAGE,
                    CLARIFYING_QUESTION,
                    AUTO_REPLY
            );

    private final String description;

    MessageKind(String description) {
        this.description = description;
    }

    public boolean isVisibleToCustomer() {
        return VISIBLE_TO_CUSTOMER.contains(this);
    }

    public boolean isAutomated() {
        return AUTOMATED.contains(this);
    }
}
