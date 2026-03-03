package com.dsi.support.agenticrouter.service.routing.policy;

import com.dsi.support.agenticrouter.dto.RouterResponse;

public record PolicyEvaluationState(
    RouterResponse response,
    boolean policyTriggered
) {
    public PolicyEvaluationState withResponse(
        RouterResponse updatedResponse
    ) {
        return new PolicyEvaluationState(
            updatedResponse,
            policyTriggered
        );
    }

    public PolicyEvaluationState withResponseAndTriggered(
        RouterResponse updatedResponse
    ) {
        return new PolicyEvaluationState(
            updatedResponse,
            true
        );
    }
}
