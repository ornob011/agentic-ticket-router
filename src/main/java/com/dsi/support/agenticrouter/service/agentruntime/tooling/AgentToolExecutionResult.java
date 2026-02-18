package com.dsi.support.agenticrouter.service.agentruntime.tooling;

import com.dsi.support.agenticrouter.enums.AgentToolExecutionStatus;

public record AgentToolExecutionResult(
    AgentToolExecutionStatus status
) {
    public static AgentToolExecutionResult executed() {
        return new AgentToolExecutionResult(
            AgentToolExecutionStatus.EXECUTED
        );
    }

    public static AgentToolExecutionResult skippedShadowMode() {
        return new AgentToolExecutionResult(
            AgentToolExecutionStatus.SKIPPED_SHADOW_MODE
        );
    }
}
