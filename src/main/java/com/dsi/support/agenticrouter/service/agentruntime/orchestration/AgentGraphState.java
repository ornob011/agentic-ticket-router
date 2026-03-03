package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.AgentRole;
import com.dsi.support.agenticrouter.enums.AgentTerminationReason;
import com.dsi.support.agenticrouter.enums.AgentValidationErrorCode;
import com.dsi.support.agenticrouter.service.agentruntime.safety.AgentSafetyDecision;
import com.dsi.support.agenticrouter.service.agentruntime.tooling.AgentToolExecutionResult;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class AgentGraphState {

    private final Long ticketId;
    private final RouterRequest routerRequest;
    private final Instant startedAt;
    private final List<AgentStepDecision> decisions;
    private int stepCount;
    private Long runtimeRunId;
    private String plannerRawJson;
    private RouterResponse plannedResponse;
    private AgentSafetyDecision safetyDecision;
    private AgentToolExecutionResult toolExecutionResult;
    private RouterResponse finalResponse;
    private AgentTerminationReason terminationReason;
    private boolean fallbackUsed;
    private AgentValidationErrorCode errorCode;
    private String errorMessage;
    private AgentRole actorRole;
    private AgentRole targetRole;
    private boolean handoff;
    private String handoffReason;

    public AgentGraphState(
        Long ticketId,
        RouterRequest routerRequest
    ) {
        this.ticketId = ticketId;
        this.routerRequest = routerRequest;
        this.startedAt = Instant.now();
        this.decisions = new ArrayList<>();
        this.stepCount = 0;
        this.actorRole = AgentRole.SUPERVISOR;
        this.targetRole = AgentRole.SUPERVISOR;
        this.handoff = false;
        this.handoffReason = null;
    }

    public void recordDecision(
        RouterResponse decisionResponse
    ) {
        stepCount++;
        decisions.add(
            AgentStepDecision.builder()
                             .step(stepCount)
                             .nextAction(Objects.toString(decisionResponse.getNextAction(), null))
                             .queue(Objects.toString(decisionResponse.getQueue(), null))
                             .priority(Objects.toString(decisionResponse.getPriority(), null))
                             .confidence(decisionResponse.getConfidence())
                             .internalNote(decisionResponse.getInternalNote())
                             .actorRole(Objects.toString(actorRole, null))
                             .targetRole(Objects.toString(targetRole, null))
                             .handoff(handoff)
                             .handoffReason(handoffReason)
                             .build()
        );
    }
}
