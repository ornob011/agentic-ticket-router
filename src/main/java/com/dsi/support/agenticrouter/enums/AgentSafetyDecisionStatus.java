package com.dsi.support.agenticrouter.enums;

public enum AgentSafetyDecisionStatus {
    ALLOW,
    REQUIRES_HUMAN_REVIEW;

    public boolean requiresHumanReview() {
        return this == REQUIRES_HUMAN_REVIEW;
    }
}
