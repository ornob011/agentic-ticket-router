package com.dsi.support.agenticrouter.enums;

import com.dsi.support.agenticrouter.service.agentruntime.planner.AgentPlannerDecision;

import java.util.Objects;

public enum AgentOrchestrationMode {
    SINGLE_AGENT("single_agent_planner", "single_agent_repair") {
        @Override
        public boolean shouldDelegate(
            AgentPlannerDecision supervisorDecision
        ) {
            return false;
        }
    },
    MULTI_AGENT("multi_agent_planner", "multi_agent_repair") {
        @Override
        public boolean shouldDelegate(
            AgentPlannerDecision supervisorDecision
        ) {
            boolean handoffRequested = supervisorDecision.handoff();
            boolean fallbackUsed = supervisorDecision.fallbackUsed();
            boolean hasTargetRole = Objects.nonNull(
                supervisorDecision.targetRole()
            );

            return handoffRequested
                   && !fallbackUsed
                   && hasTargetRole;
        }
    };

    private final String plannerTemplateName;
    private final String repairTemplateName;

    AgentOrchestrationMode(
        String plannerTemplateName,
        String repairTemplateName
    ) {
        this.plannerTemplateName = plannerTemplateName;
        this.repairTemplateName = repairTemplateName;
    }

    public String plannerTemplateName() {
        return plannerTemplateName;
    }

    public String repairTemplateName() {
        return repairTemplateName;
    }

    public abstract boolean shouldDelegate(
        AgentPlannerDecision supervisorDecision
    );
}
