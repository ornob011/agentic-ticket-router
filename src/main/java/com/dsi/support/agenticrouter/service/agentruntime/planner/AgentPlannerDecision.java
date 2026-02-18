package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.AgentValidationErrorCode;

public record AgentPlannerDecision(
    String plannerRawJson,
    RouterResponse routerResponse,
    boolean fallbackUsed,
    AgentValidationErrorCode errorCode,
    String errorMessage
) {
}
