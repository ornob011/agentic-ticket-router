package com.dsi.support.agenticrouter.enums;

public enum AgentRuntimeGraphNode {
    PLAN("plan"),
    SAFETY("safety"),
    TOOL_EXECUTION("tool_execution"),
    REFLECT("reflect"),
    TERMINATE("terminate");

    private final String id;

    AgentRuntimeGraphNode(
        String id
    ) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
