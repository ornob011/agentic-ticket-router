package com.dsi.support.agenticrouter.service.agentruntime.trace;

import com.dsi.support.agenticrouter.enums.AgentRuntimeRunStatus;
import com.dsi.support.agenticrouter.enums.AgentTerminationReason;

public record AgentRuntimeRunFinishCommand(
    Long runId,
    AgentRuntimeRunStatus status,
    AgentTerminationReason terminationReason,
    int totalSteps,
    boolean fallbackUsed,
    String errorCode,
    String errorMessage
) {
}
