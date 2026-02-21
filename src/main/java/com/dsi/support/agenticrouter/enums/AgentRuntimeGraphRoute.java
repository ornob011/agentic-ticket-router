package com.dsi.support.agenticrouter.enums;

public enum AgentRuntimeGraphRoute {
    TO_SAFETY("to_safety"),
    TO_TOOL_EXECUTION("to_tool_execution"),
    TO_REFLECT("to_reflect"),
    TO_PLAN("to_plan"),
    TO_TERMINATE("to_terminate");

    private final String id;

    AgentRuntimeGraphRoute(
        String id
    ) {
        this.id = id;
    }

    public static AgentRuntimeGraphRoute fromId(
        String id
    ) {
        for (AgentRuntimeGraphRoute route : values()) {
            if (route.id.equals(id)) {
                return route;
            }
        }

        return TO_TERMINATE;
    }

    public String id() {
        return id;
    }
}
