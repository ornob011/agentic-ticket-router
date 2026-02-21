package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

@Getter
public enum MemoryScope {
    TICKET("ticket", "Current ticket conversation history"),
    CUSTOMER("customer", "Cross-ticket customer context and preferences"),
    AGENT("agent", "Agent's learned patterns per customer"),


    ;

    private final String prefix;
    private final String description;

    MemoryScope(
        String prefix,
        String description
    ) {
        this.prefix = prefix;
        this.description = description;
    }

    public String formatConversationId(
        Long identifier
    ) {
        return prefix + ":" + identifier;
    }

    public String formatConversationId(
        String identifier
    ) {
        return prefix + ":" + identifier;
    }
}
