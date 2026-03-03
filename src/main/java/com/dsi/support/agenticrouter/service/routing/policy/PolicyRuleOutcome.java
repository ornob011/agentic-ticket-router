package com.dsi.support.agenticrouter.service.routing.policy;

public record PolicyRuleOutcome(
    PolicyEvaluationState state,
    boolean changed
) {
    public static PolicyRuleOutcome unchanged(
        PolicyEvaluationState state
    ) {
        return new PolicyRuleOutcome(
            state,
            false
        );
    }

    public static PolicyRuleOutcome changed(
        PolicyEvaluationState state
    ) {
        return new PolicyRuleOutcome(
            state,
            true
        );
    }
}
