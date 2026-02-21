package com.dsi.support.agenticrouter.enums;

public enum AgentPolicyReason {
    NEXT_ACTION_OVERRIDDEN("next_action_overridden"),
    QUEUE_OVERRIDDEN("queue_overridden"),
    PRIORITY_OVERRIDDEN("priority_overridden"),
    CATEGORY_OVERRIDDEN("category_overridden"),
    HUMAN_INTERVENTION_REQUIRED("human_intervention_required"),
    HIGH_RISK_CATEGORY("high_risk_category");

    private final String code;

    AgentPolicyReason(
        String code
    ) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
