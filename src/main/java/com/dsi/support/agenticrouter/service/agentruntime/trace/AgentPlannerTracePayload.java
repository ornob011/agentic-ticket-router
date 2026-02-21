package com.dsi.support.agenticrouter.service.agentruntime.trace;

import com.dsi.support.agenticrouter.service.agentruntime.planner.AgentPlannerDecision;

public record AgentPlannerTracePayload(
    AgentPlannerDecision supervisorDecision,
    AgentPlannerDecision plannerDecision,
    String plannerRawJson
) {
}
