package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import com.dsi.support.agenticrouter.enums.AgentRuntimeGraphRoute;
import com.dsi.support.agenticrouter.enums.AgentRuntimeStateKey;
import lombok.Builder;

import java.util.Map;

@Builder
public record AgentRuntimeStateUpdate(
    int stepCount,
    boolean terminated,
    AgentRuntimeGraphRoute nextRoute
) {
    public static AgentRuntimeStateUpdate initial() {
        return AgentRuntimeStateUpdate.builder()
                                      .stepCount(0)
                                      .terminated(false)
                                      .nextRoute(AgentRuntimeGraphRoute.TO_SAFETY)
                                      .build();
    }

    public Map<String, Object> asMap() {
        return Map.of(
            AgentRuntimeStateKey.STEP_COUNT.key(),
            stepCount,
            AgentRuntimeStateKey.TERMINATED.key(),
            terminated,
            AgentRuntimeStateKey.NEXT_ROUTE.key(),
            nextRoute.id()
        );
    }
}
