package com.dsi.support.agenticrouter.service.routing.policy;

import com.dsi.support.agenticrouter.enums.RoutingPolicyRuleCode;

public interface RouterPolicyRule {

    RoutingPolicyRuleCode code();

    PolicyRuleOutcome apply(
        PolicyEvaluationState state
    );
}
