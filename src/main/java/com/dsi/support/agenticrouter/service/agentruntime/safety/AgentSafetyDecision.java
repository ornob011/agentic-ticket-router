package com.dsi.support.agenticrouter.service.agentruntime.safety;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.AgentSafetyDecisionStatus;

import java.util.List;

public record AgentSafetyDecision(
    AgentSafetyDecisionStatus status,
    RouterResponse response,
    boolean policyOverridden,
    List<String> policyReasons
) {
}
