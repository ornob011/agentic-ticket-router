package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AgentStepDecision {
    private int step;
    private String nextAction;
    private String queue;
    private String priority;
    private BigDecimal confidence;
    private String internalNote;
    private String actorRole;
    private String targetRole;
    private boolean handoff;
    private String handoffReason;
}
