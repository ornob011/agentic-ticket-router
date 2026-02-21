package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.AgentRole;
import com.dsi.support.agenticrouter.enums.AgentValidationErrorCode;

public record AgentPlannerDecision(
    String plannerRawJson,
    RouterResponse routerResponse,
    AgentRole actorRole,
    AgentRole targetRole,
    boolean handoff,
    String handoffReason,
    boolean fallbackUsed,
    AgentValidationErrorCode errorCode,
    String errorMessage
) {
}
