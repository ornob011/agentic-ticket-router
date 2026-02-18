package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.enums.AgentValidationErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record AgentPlanValidationResult(
    boolean valid,
    AgentValidationErrorCode errorCode,
    String errorMessage,
    JsonNode jsonNode
) {
}
