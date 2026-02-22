package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.configuration.AgentRuntimeConfiguration;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.AgentRole;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.service.routing.RouterResponseContractValidator;
import com.dsi.support.agenticrouter.util.AgentRuntimeConstants;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPlannerClient {

    private final AgentPlannerLlmClient agentPlannerLlmClient;
    private final AgentPlanSchemaValidator agentPlanSchemaValidator;
    private final RouterResponseContractValidator routerResponseContractValidator;
    private final AgentRuntimeFallbackService agentRuntimeFallbackService;
    private final AgentRuntimeConfiguration agentRuntimeConfiguration;

    public AgentPlannerDecision decide(
        RouterRequest routerRequest,
        Long ticketId
    ) {
        return decideForRole(
            routerRequest,
            ticketId,
            AgentRole.SUPERVISOR
        );
    }

    public AgentPlannerDecision decideForRole(
        RouterRequest routerRequest,
        Long ticketId,
        AgentRole actorRole
    ) {
        log.info(
            "AgentPlannerClient({}) SupportTicket(id:{}) Planner(role:{}) Outcome(start)",
            OperationalLogContext.PHASE_START,
            ticketId,
            actorRole
        );

        String plannerRawJson = agentPlannerLlmClient.requestPlan(
            routerRequest,
            ticketId,
            actorRole
        );

        String candidateJson = plannerRawJson;

        AgentPlanValidationResult validationResult = agentRuntimeConfiguration.isSchemaEnforcementEnabled()
            ? agentPlanSchemaValidator.validate(candidateJson)
            : AgentPlanValidationResult.builder()
                                       .valid(true)
                                       .jsonNode(null)
                                       .build();

        int retries = 0;
        int maxRetries = agentRuntimeConfiguration.getPlannerValidationRetries();
        while (agentRuntimeConfiguration.isRepairEnabled()
               && agentRuntimeConfiguration.isSchemaEnforcementEnabled()
               && !validationResult.valid()
               && retries < maxRetries
        ) {
            log.warn(
                "AgentPlannerRetry(decision) SupportTicket(id:{}) Outcome(retry:{},errorCode:{},error:{})",
                ticketId,
                retries + 1,
                validationResult.errorCode(),
                validationResult.errorMessage()
            );

            candidateJson = agentPlannerLlmClient.repairPlan(
                candidateJson,
                validationResult.errorMessage(),
                routerRequest,
                ticketId,
                actorRole
            );
            validationResult = agentPlanSchemaValidator.validate(
                candidateJson
            );
            retries++;
        }

        if (agentRuntimeConfiguration.isSchemaEnforcementEnabled() && !validationResult.valid()) {
            log.error(
                "AgentPlannerFallback(fail) SupportTicket(id:{}) Outcome(errorCode:{},error:{})",
                ticketId,
                validationResult.errorCode(),
                validationResult.errorMessage()
            );

            return new AgentPlannerDecision(
                plannerRawJson,
                agentRuntimeFallbackService.humanReviewFallback(
                    AgentRuntimeConstants.PLANNER_VALIDATION_FAILED_PREFIX + validationResult.errorMessage()
                ),
                actorRole,
                actorRole,
                false,
                null,
                true,
                validationResult.errorCode(),
                validationResult.errorMessage()
            );
        }

        RouterResponse routerResponse = agentRuntimeConfiguration.isSchemaEnforcementEnabled()
            ? agentPlanSchemaValidator.map(validationResult.jsonNode())
            : agentPlannerLlmClient.mapUnchecked(candidateJson);

        routerResponseContractValidator.validate(
            routerResponse
        );

        AgentRole targetRole = resolveTargetRole(
            actorRole,
            routerResponse
        );

        boolean handoff = actorRole == AgentRole.SUPERVISOR
                          && targetRole != AgentRole.SUPERVISOR;

        AgentPlannerDecision plannerDecision = new AgentPlannerDecision(
            plannerRawJson,
            routerResponse,
            actorRole,
            targetRole,
            handoff,
            handoff ? AgentRuntimeConstants.HANDOFF_REASON_SUPERVISOR_DELEGATION : null,
            false,
            null,
            null
        );

        log.info(
            "AgentPlannerClient({}) SupportTicket(id:{}) Planner(role:{}) Outcome(nextAction:{},targetRole:{},handoff:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            actorRole,
            plannerDecision.routerResponse().getNextAction(),
            plannerDecision.targetRole(),
            plannerDecision.handoff()
        );

        return plannerDecision;
    }

    private AgentRole resolveTargetRole(
        AgentRole actorRole,
        RouterResponse routerResponse
    ) {
        if (actorRole != AgentRole.SUPERVISOR) {
            return actorRole;
        }

        NextAction nextAction = routerResponse.getNextAction();

        return switch (nextAction) {
            case ASK_CLARIFYING -> AgentRole.QA;
            case HUMAN_REVIEW, ESCALATE, AUTO_ESCALATE -> AgentRole.ESCALATOR;
            case UPDATE_CUSTOMER_PROFILE, USE_KNOWLEDGE_ARTICLE, USE_TEMPLATE, AUTO_REPLY, AUTO_RESOLVE ->
                AgentRole.RESOLVER;
            case ASSIGN_QUEUE, CHANGE_PRIORITY, ADD_INTERNAL_NOTE, TRIGGER_NOTIFICATION, REOPEN_TICKET ->
                AgentRole.CLASSIFIER;
        };
    }
}
