package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import com.dsi.support.agenticrouter.configuration.AgentRuntimeConfiguration;
import com.dsi.support.agenticrouter.enums.AgentOrchestrationMode;
import com.dsi.support.agenticrouter.service.agentruntime.planner.AgentPlannerDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentOrchestrationModeResolver {

    private final AgentRuntimeConfiguration agentRuntimeConfiguration;

    public AgentOrchestrationMode mode() {
        return agentRuntimeConfiguration.getOrchestrationMode();
    }

    public boolean shouldDelegate(
        AgentPlannerDecision supervisorDecision
    ) {
        return mode().shouldDelegate(
            supervisorDecision
        );
    }
}
