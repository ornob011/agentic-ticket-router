package com.dsi.support.agenticrouter.enums;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Optional;

public enum AgentRuntimeStateKey {
    STEP_COUNT("step_count"),
    TERMINATED("terminated"),
    NEXT_ROUTE("next_route");

    private final String key;

    AgentRuntimeStateKey(
        String key
    ) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public <T> Optional<T> read(
        AgentState state
    ) {
        return state.value(
            key
        );
    }
}
