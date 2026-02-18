package com.dsi.support.agenticrouter.service.agentruntime.trace;

import com.dsi.support.agenticrouter.enums.AgentRuntimeStepType;

public record AgentRuntimeStepTraceCommand(
    Long runId,
    int stepNo,
    AgentRuntimeStepType stepType,
    Object plannerOutput,
    Object validatedResponse,
    Object safetyDecision,
    Object toolResult,
    Long latencyMs,
    boolean success,
    String errorCode,
    String errorMessage
) {
}
