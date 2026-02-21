package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import com.dsi.support.agenticrouter.enums.AgentRuntimeGraphRoute;
import com.dsi.support.agenticrouter.enums.AgentRuntimeStateKey;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AgentRuntimeStateNavigator {

    public AgentRuntimeStateUpdate initialState() {
        return AgentRuntimeStateUpdate.initial();
    }

    public boolean isTerminated(
        AgentState agentState
    ) {
        return AgentRuntimeStateKey.TERMINATED.<Boolean>read(
            agentState
        ).orElse(false);
    }

    @NonNull
    public AgentRuntimeGraphRoute nextRoute(
        AgentState agentState
    ) {
        return AgentRuntimeStateKey.NEXT_ROUTE.<String>read(agentState)
                                              .map(AgentRuntimeGraphRoute::fromId)
                                              .orElse(AgentRuntimeGraphRoute.TO_TERMINATE);
    }
}
