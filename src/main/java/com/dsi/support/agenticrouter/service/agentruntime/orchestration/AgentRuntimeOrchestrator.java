package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import com.dsi.support.agenticrouter.configuration.AgentRuntimeConfiguration;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.AgentRuntimeRun;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.service.agentruntime.planner.AgentPlannerClient;
import com.dsi.support.agenticrouter.service.agentruntime.planner.AgentPlannerDecision;
import com.dsi.support.agenticrouter.service.agentruntime.safety.AgentSafetyDecision;
import com.dsi.support.agenticrouter.service.agentruntime.safety.AgentSafetyEvaluator;
import com.dsi.support.agenticrouter.service.agentruntime.tooling.AgentToolExecutionResult;
import com.dsi.support.agenticrouter.service.agentruntime.tooling.AgentToolExecutor;
import com.dsi.support.agenticrouter.service.agentruntime.trace.AgentPlannerTracePayload;
import com.dsi.support.agenticrouter.service.agentruntime.trace.AgentRuntimeRunFinishCommand;
import com.dsi.support.agenticrouter.service.agentruntime.trace.AgentRuntimeStepTraceCommand;
import com.dsi.support.agenticrouter.service.agentruntime.trace.AgentRuntimeTraceService;
import com.dsi.support.agenticrouter.service.ticket.TicketRoutingPersistenceService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRuntimeOrchestrator {

    private final AgentPlannerClient agentPlannerClient;
    private final AgentSafetyEvaluator agentSafetyEvaluator;
    private final AgentRuntimeTerminationPolicy terminationPolicy;
    private final AgentToolExecutor agentToolExecutor;
    private final AgentRuntimeConfiguration agentRuntimeConfiguration;
    private final AgentRuntimeStateNavigator stateNavigator;
    private final AgentDecisionValidator agentDecisionValidator;
    private final AgentRuntimeTraceService agentRuntimeTraceService;
    private final TicketRoutingPersistenceService ticketRoutingPersistenceService;
    private final AgentOrchestrationModeResolver agentOrchestrationModeResolver;

    public RouterResponse execute(
        SupportTicket supportTicket,
        RouterRequest routerRequest
    ) throws GraphStateException {
        log.info(
            "AgentRuntime({}) SupportTicket(id:{},status:{}) Outcome(started)",
            OperationalLogContext.PHASE_START,
            supportTicket.getId(),
            supportTicket.getStatus()
        );

        AgentRuntimeRun startedRuntimeRun = agentRuntimeTraceService.startRun(
            supportTicket.getId()
        );

        AgentGraphState agentGraphState = new AgentGraphState(
            supportTicket.getId(),
            routerRequest
        );

        agentGraphState.setRuntimeRunId(
            startedRuntimeRun.getId()
        );

        StateGraph<AgentState> stateGraph = new StateGraph<>(AgentState::new);

        stateGraph.addNode(
            AgentRuntimeGraphNode.PLAN.id(),
            AsyncNodeActionWithConfig.node_async(
                (state, config) -> runPlanNode(
                    supportTicket,
                    routerRequest,
                    agentGraphState
                ).asMap()
            )
        );

        stateGraph.addNode(
            AgentRuntimeGraphNode.SAFETY.id(),
            AsyncNodeActionWithConfig.node_async(
                (state, config) -> runSafetyNode(
                    supportTicket,
                    agentGraphState
                ).asMap()
            )
        );

        stateGraph.addNode(
            AgentRuntimeGraphNode.TOOL_EXECUTION.id(),
            AsyncNodeActionWithConfig.node_async(
                (state, config) -> runToolExecutionNode(
                    supportTicket,
                    agentGraphState
                ).asMap()
            )
        );

        stateGraph.addNode(
            AgentRuntimeGraphNode.REFLECT.id(),
            AsyncNodeActionWithConfig.node_async(
                (state, config) -> runReflectNode(
                    supportTicket,
                    agentGraphState
                ).asMap()
            )
        );

        stateGraph.addNode(
            AgentRuntimeGraphNode.TERMINATE.id(),
            AsyncNodeActionWithConfig.node_async(
                (state, config) -> runTerminateNode(
                    agentGraphState
                ).asMap()
            )
        );

        stateGraph.addEdge(
            StateGraph.START,
            AgentRuntimeGraphNode.PLAN.id()
        );

        stateGraph.addEdge(
            AgentRuntimeGraphNode.PLAN.id(),
            AgentRuntimeGraphNode.SAFETY.id()
        );

        stateGraph.addEdge(
            AgentRuntimeGraphNode.SAFETY.id(),
            AgentRuntimeGraphNode.TOOL_EXECUTION.id()
        );

        stateGraph.addEdge(
            AgentRuntimeGraphNode.TOOL_EXECUTION.id(),
            AgentRuntimeGraphNode.REFLECT.id()
        );

        stateGraph.addConditionalEdges(
            AgentRuntimeGraphNode.REFLECT.id(),
            state -> CompletableFuture.completedFuture(
                stateNavigator.nextRoute(state).id()
            ),
            java.util.Map.of(
                AgentRuntimeGraphRoute.TO_PLAN.id(),
                AgentRuntimeGraphNode.PLAN.id(),
                AgentRuntimeGraphRoute.TO_TERMINATE.id(),
                AgentRuntimeGraphNode.TERMINATE.id()
            )
        );

        stateGraph.addEdge(
            AgentRuntimeGraphNode.TERMINATE.id(),
            StateGraph.END
        );

        CompiledGraph<AgentState> compiledGraph = stateGraph.compile();

        compiledGraph.setMaxIterations(
            Math.max(
                1,
                agentRuntimeConfiguration.getMaxSteps() * 8
            )
        );

        AgentRuntimeRunStatus runStatus = AgentRuntimeRunStatus.FAILED;

        AgentTerminationReason terminationReason = AgentTerminationReason.PLAN_VALIDATION_FAILED;

        try {
            compiledGraph.invoke(
                stateNavigator.initialState().asMap()
            );

            runStatus = AgentRuntimeRunStatus.COMPLETED;

            terminationReason = Objects.requireNonNullElse(
                agentGraphState.getTerminationReason(),
                AgentTerminationReason.GOAL_REACHED
            );

            return Objects.requireNonNull(
                agentGraphState.getFinalResponse(),
                "agentRuntime.finalResponse"
            );
        } finally {
            agentRuntimeTraceService.finishRun(
                new AgentRuntimeRunFinishCommand(
                    startedRuntimeRun.getId(),
                    runStatus,
                    terminationReason,
                    agentGraphState.getStepCount(),
                    agentGraphState.isFallbackUsed(),
                    errorCodeName(agentGraphState.getErrorCode()),
                    agentGraphState.getErrorMessage()
                )
            );

            log.info(
                "AgentRuntime({}) SupportTicket(id:{},status:{}) Outcome(ended,runStatus:{},terminationReason:{})",
                OperationalLogContext.PHASE_COMPLETE,
                supportTicket.getId(),
                supportTicket.getStatus(),
                runStatus,
                terminationReason
            );
        }
    }

    private AgentRuntimeStateUpdate runPlanNode(
        SupportTicket supportTicket,
        RouterRequest routerRequest,
        AgentGraphState agentGraphState
    ) {
        long startedAt = System.currentTimeMillis();

        AgentPlannerDecision supervisorDecision = agentPlannerClient.decide(
            routerRequest,
            supportTicket.getId()
        );

        AgentPlannerDecision plannerDecision = resolvePlannerDecision(
            supportTicket,
            routerRequest,
            agentGraphState,
            supervisorDecision
        );

        RouterResponse plannedResponse = plannerDecision.routerResponse();

        agentDecisionValidator.validate(
            plannedResponse
        );

        agentGraphState.setPlannerRawJson(
            plannerDecision.plannerRawJson()
        );

        agentGraphState.setFallbackUsed(
            plannerDecision.fallbackUsed()
        );

        agentGraphState.setErrorCode(
            plannerDecision.errorCode()
        );

        agentGraphState.setErrorMessage(
            plannerDecision.errorMessage()
        );

        agentGraphState.setPlannedResponse(
            plannedResponse
        );

        log.info(
            "AgentRuntime({}) SupportTicket(id:{},status:{}) Plan(actorRole:{},targetRole:{},handoff:{},nextAction:{},queue:{},priority:{},confidence:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            agentGraphState.getActorRole(),
            agentGraphState.getTargetRole(),
            agentGraphState.isHandoff(),
            plannedResponse.getNextAction(),
            plannedResponse.getQueue(),
            plannedResponse.getPriority(),
            plannedResponse.getConfidence()
        );

        agentRuntimeTraceService.recordStep(
            new AgentRuntimeStepTraceCommand(
                agentGraphState.getRuntimeRunId(),
                Math.max(agentGraphState.getStepCount(), 1),
                AgentRuntimeStepType.PLAN,
                new AgentPlannerTracePayload(
                    supervisorDecision,
                    plannerDecision,
                    agentGraphState.getPlannerRawJson()
                ),
                plannedResponse,
                null,
                null,
                System.currentTimeMillis() - startedAt,
                true,
                errorCodeName(plannerDecision.errorCode()),
                plannerDecision.errorMessage(),
                agentGraphState.getActorRole(),
                agentGraphState.getTargetRole(),
                agentGraphState.isHandoff(),
                agentGraphState.getHandoffReason()
            )
        );

        return AgentRuntimeStateUpdate.builder()
                                      .stepCount(agentGraphState.getStepCount())
                                      .terminated(false)
                                      .nextRoute(AgentRuntimeGraphRoute.TO_SAFETY)
                                      .build();
    }

    private AgentPlannerDecision resolvePlannerDecision(
        SupportTicket supportTicket,
        RouterRequest routerRequest,
        AgentGraphState agentGraphState,
        AgentPlannerDecision supervisorDecision
    ) {
        AgentPlannerDecision plannerDecision = supervisorDecision;

        boolean shouldDelegateByRole = agentOrchestrationModeResolver.shouldDelegate(
            supervisorDecision
        );

        log.info(
            "AgentRuntime({}) SupportTicket(id:{}) PlanFlow(mode:{}) Outcome(delegate:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            agentOrchestrationModeResolver.mode(),
            shouldDelegateByRole
        );

        if (shouldDelegateByRole) {
            log.info(
                "AgentRuntime({}) SupportTicket(id:{}) PlanDelegate(start) Outcome(supervisorAction:{},targetRole:{})",
                OperationalLogContext.PHASE_START,
                supportTicket.getId(),
                supervisorDecision.routerResponse().getNextAction(),
                supervisorDecision.targetRole()
            );

            plannerDecision = agentPlannerClient.decideForRole(
                routerRequest,
                supportTicket.getId(),
                supervisorDecision.targetRole(),
                supervisorDecision.routerResponse().getNextAction()
            );

            applyHandoffState(
                agentGraphState,
                plannerDecision,
                supervisorDecision
            );

            log.info(
                "AgentRuntime({}) SupportTicket(id:{}) PlanDelegate(handoff:{},fallbackUsed:{},targetRole:{}) Outcome(delegated:true)",
                OperationalLogContext.PHASE_DECISION,
                supportTicket.getId(),
                supervisorDecision.handoff(),
                supervisorDecision.fallbackUsed(),
                supervisorDecision.targetRole()
            );

            return plannerDecision;
        }

        applyPlannerState(
            agentGraphState,
            plannerDecision
        );

        log.info(
            "AgentRuntime({}) SupportTicket(id:{}) PlanDelegate(handoff:{},fallbackUsed:{},targetRole:{}) Outcome(delegated:false)",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supervisorDecision.handoff(),
            supervisorDecision.fallbackUsed(),
            supervisorDecision.targetRole()
        );

        return plannerDecision;
    }

    private void applyPlannerState(
        AgentGraphState agentGraphState,
        AgentPlannerDecision plannerDecision
    ) {
        agentGraphState.setActorRole(
            plannerDecision.actorRole()
        );

        agentGraphState.setTargetRole(
            plannerDecision.targetRole()
        );

        agentGraphState.setHandoff(
            plannerDecision.handoff()
        );

        agentGraphState.setHandoffReason(
            plannerDecision.handoffReason()
        );
    }

    private void applyHandoffState(
        AgentGraphState agentGraphState,
        AgentPlannerDecision plannerDecision,
        AgentPlannerDecision supervisorDecision
    ) {
        agentGraphState.setActorRole(
            plannerDecision.actorRole()
        );

        agentGraphState.setTargetRole(
            plannerDecision.targetRole()
        );

        agentGraphState.setHandoff(
            true
        );

        agentGraphState.setHandoffReason(
            supervisorDecision.handoffReason()
        );
    }

    private AgentRuntimeStateUpdate runSafetyNode(
        SupportTicket supportTicket,
        AgentGraphState agentGraphState
    ) {
        long startedAt = System.currentTimeMillis();

        AgentSafetyDecision safetyDecision = agentSafetyEvaluator.evaluate(
            agentGraphState.getPlannedResponse()
        );

        RouterResponse safeResponse = safetyDecision.response();

        agentDecisionValidator.validate(
            safeResponse
        );

        agentGraphState.setSafetyDecision(
            safetyDecision
        );

        agentGraphState.recordDecision(
            safeResponse
        );

        agentGraphState.setFinalResponse(
            safeResponse
        );

        ticketRoutingPersistenceService.applyRoutingDecision(
            supportTicket,
            safeResponse
        );

        log.info(
            "AgentRuntime({}) SupportTicket(id:{},status:{}) Safety(status:{},nextAction:{},queue:{},priority:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            safetyDecision.status(),
            safeResponse.getNextAction(),
            safeResponse.getQueue(),
            safeResponse.getPriority()
        );

        agentRuntimeTraceService.recordStep(
            new AgentRuntimeStepTraceCommand(
                agentGraphState.getRuntimeRunId(),
                agentGraphState.getStepCount(),
                AgentRuntimeStepType.SAFETY,
                null,
                safeResponse,
                safetyDecision,
                null,
                System.currentTimeMillis() - startedAt,
                true,
                null,
                null,
                agentGraphState.getActorRole(),
                agentGraphState.getTargetRole(),
                agentGraphState.isHandoff(),
                agentGraphState.getHandoffReason()
            )
        );

        return AgentRuntimeStateUpdate.builder()
                                      .stepCount(agentGraphState.getStepCount())
                                      .terminated(false)
                                      .nextRoute(AgentRuntimeGraphRoute.TO_TOOL_EXECUTION)
                                      .build();
    }

    private AgentRuntimeStateUpdate runToolExecutionNode(
        SupportTicket supportTicket,
        AgentGraphState agentGraphState
    ) throws BindException {
        long startedAt = System.currentTimeMillis();

        RouterResponse safeResponse = Objects.requireNonNull(
            agentGraphState.getFinalResponse(),
            "agentRuntime.safeResponse"
        );

        if (!agentRuntimeConfiguration.isShadowMode()) {
            agentToolExecutor.execute(
                supportTicket,
                safeResponse
            );

            agentGraphState.setToolExecutionResult(
                AgentToolExecutionResult.executed()
            );
        } else {
            agentGraphState.setToolExecutionResult(
                AgentToolExecutionResult.skippedShadowMode()
            );
        }

        log.info(
            "AgentRuntime({}) SupportTicket(id:{},status:{}) Tool(status:{},step:{},nextAction:{},queue:{},priority:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            agentGraphState.getToolExecutionResult().status(),
            agentGraphState.getStepCount(),
            safeResponse.getNextAction(),
            safeResponse.getQueue(),
            safeResponse.getPriority()
        );

        agentRuntimeTraceService.recordStep(
            new AgentRuntimeStepTraceCommand(
                agentGraphState.getRuntimeRunId(),
                agentGraphState.getStepCount(),
                AgentRuntimeStepType.ACT,
                null,
                safeResponse,
                null,
                agentGraphState.getToolExecutionResult(),
                System.currentTimeMillis() - startedAt,
                true,
                null,
                null,
                agentGraphState.getActorRole(),
                agentGraphState.getTargetRole(),
                agentGraphState.isHandoff(),
                agentGraphState.getHandoffReason()
            )
        );

        return AgentRuntimeStateUpdate.builder()
                                      .stepCount(agentGraphState.getStepCount())
                                      .terminated(false)
                                      .nextRoute(AgentRuntimeGraphRoute.TO_REFLECT)
                                      .build();
    }

    private AgentRuntimeStateUpdate runReflectNode(
        SupportTicket supportTicket,
        AgentGraphState agentGraphState
    ) {
        long startedAt = System.currentTimeMillis();

        RouterResponse safeResponse = Objects.requireNonNull(
            agentGraphState.getFinalResponse(),
            "agentRuntime.safeResponse"
        );

        AgentSafetyDecision safetyDecision = Objects.requireNonNull(
            agentGraphState.getSafetyDecision(),
            "agentRuntime.safetyDecision"
        );

        boolean isPolicyReached = terminationPolicy.shouldTerminate(agentGraphState);
        boolean needsReview = safetyDecision.status().requiresHumanReview();
        boolean actionBlocked = safeResponse.getNextAction().requiresHumanIntervention();

        boolean shouldTerminate = isPolicyReached || needsReview || actionBlocked || agentGraphState.getStepCount() > 0;

        AgentRuntimeGraphRoute nextRoute = AgentRuntimeGraphRoute.TO_PLAN;

        if (shouldTerminate) {
            nextRoute = AgentRuntimeGraphRoute.TO_TERMINATE;
        }

        if (shouldTerminate) {
            agentGraphState.setTerminationReason(
                resolveTerminationReason(
                    safetyDecision,
                    agentGraphState
                )
            );
        }

        log.info(
            "AgentRuntime({}) SupportTicket(id:{},status:{}) Reflect(step:{},terminate:{},nextRoute:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            agentGraphState.getStepCount(),
            shouldTerminate,
            nextRoute
        );

        agentRuntimeTraceService.recordStep(
            new AgentRuntimeStepTraceCommand(
                agentGraphState.getRuntimeRunId(),
                agentGraphState.getStepCount(),
                AgentRuntimeStepType.REFLECT,
                null,
                safeResponse,
                safetyDecision,
                agentGraphState.getToolExecutionResult(),
                System.currentTimeMillis() - startedAt,
                true,
                null,
                null,
                agentGraphState.getActorRole(),
                agentGraphState.getTargetRole(),
                agentGraphState.isHandoff(),
                agentGraphState.getHandoffReason()
            )
        );

        return AgentRuntimeStateUpdate.builder()
                                      .stepCount(agentGraphState.getStepCount())
                                      .terminated(shouldTerminate)
                                      .nextRoute(nextRoute)
                                      .build();
    }

    private AgentRuntimeStateUpdate runTerminateNode(
        AgentGraphState agentGraphState
    ) {
        agentRuntimeTraceService.recordStep(
            new AgentRuntimeStepTraceCommand(
                agentGraphState.getRuntimeRunId(),
                agentGraphState.getStepCount(),
                AgentRuntimeStepType.TERMINATE,
                null,
                agentGraphState.getFinalResponse(),
                agentGraphState.getSafetyDecision(),
                agentGraphState.getToolExecutionResult(),
                0L,
                true,
                null,
                null,
                agentGraphState.getActorRole(),
                agentGraphState.getTargetRole(),
                agentGraphState.isHandoff(),
                agentGraphState.getHandoffReason()
            )
        );

        return AgentRuntimeStateUpdate.builder()
                                      .stepCount(agentGraphState.getStepCount())
                                      .terminated(true)
                                      .nextRoute(AgentRuntimeGraphRoute.TO_TERMINATE)
                                      .build();
    }

    private AgentTerminationReason resolveTerminationReason(
        AgentSafetyDecision safetyDecision,
        AgentGraphState agentGraphState
    ) {
        if (agentGraphState.isFallbackUsed()) {
            return AgentTerminationReason.PLAN_VALIDATION_FAILED;
        }

        if (safetyDecision.status().requiresHumanReview()) {
            return AgentTerminationReason.SAFETY_BLOCKED;
        }

        if (terminationPolicy.shouldTerminate(agentGraphState)) {
            return AgentTerminationReason.BUDGET_EXCEEDED;
        }

        return AgentTerminationReason.GOAL_REACHED;
    }

    private String errorCodeName(
        Enum<?> errorCode
    ) {
        if (errorCode == null) {
            return null;
        }

        return errorCode.name();
    }
}
