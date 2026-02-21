package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum AgentRole {
    SUPERVISOR("Supervisor", "Routes tickets to specialist agents based on context", AgentType.ORCHESTRATOR),
    CLASSIFIER("Classifier", "Categorizes and prioritizes incoming tickets", AgentType.SPECIALIST),
    RESOLVER("Resolver", "Attempts automatic resolution using knowledge base and tools", AgentType.SPECIALIST),
    QA("QA Agent", "Asks clarifying questions when ticket context is ambiguous", AgentType.SPECIALIST),
    ESCALATOR("Escalator", "Handles escalation and human review decisions", AgentType.SPECIALIST),


    ;

    private static final Set<AgentRole> SPECIALIST_AGENTS =
        EnumSet.of(
            CLASSIFIER,
            RESOLVER,
            QA,
            ESCALATOR
        );

    private final String displayName;
    private final String description;
    private final AgentType type;

    AgentRole(
        String displayName,
        String description,
        AgentType type
    ) {
        this.displayName = displayName;
        this.description = description;
        this.type = type;
    }

    public boolean isSpecialist() {
        return SPECIALIST_AGENTS.contains(this);
    }

    public boolean isOrchestrator() {
        return type == AgentType.ORCHESTRATOR;
    }

    public boolean canExecuteTools() {
        return this == RESOLVER || this == QA;
    }

    public boolean canRoute() {
        return this == SUPERVISOR;
    }

    @Getter
    public enum AgentType {
        ORCHESTRATOR("orchestrator"),
        SPECIALIST("specialist");

        private final String type;

        AgentType(String type) {
            this.type = type;
        }
    }
}
