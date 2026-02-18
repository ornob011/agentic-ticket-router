package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.configuration.AgentRuntimeConfiguration;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.service.routing.RouterResponseContractValidator;
import com.dsi.support.agenticrouter.util.AgentRuntimeConstants;
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
        String plannerRaw = agentPlannerLlmClient.requestPlan(
            routerRequest,
            ticketId
        );

        String candidate = plannerRaw;
        if (agentRuntimeConfiguration.isRepairEnabled()) {
            candidate = agentPlannerLlmClient.repairPlan(
                plannerRaw,
                null,
                routerRequest,
                ticketId
            );
        }

        AgentPlanValidationResult validationResult = agentRuntimeConfiguration.isSchemaEnforcementEnabled()
            ? agentPlanSchemaValidator.validate(candidate)
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

            candidate = agentPlannerLlmClient.repairPlan(
                candidate,
                validationResult.errorMessage(),
                routerRequest,
                ticketId
            );
            validationResult = agentPlanSchemaValidator.validate(
                candidate
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
                plannerRaw,
                agentRuntimeFallbackService.humanReviewFallback(
                    AgentRuntimeConstants.PLANNER_VALIDATION_FAILED_PREFIX + validationResult.errorMessage()
                ),
                true,
                validationResult.errorCode(),
                validationResult.errorMessage()
            );
        }

        RouterResponse routerResponse = agentRuntimeConfiguration.isSchemaEnforcementEnabled()
            ? agentPlanSchemaValidator.map(validationResult.jsonNode())
            : agentPlannerLlmClient.mapUnchecked(candidate);

        routerResponseContractValidator.validate(
            routerResponse
        );

        return new AgentPlannerDecision(
            plannerRaw,
            routerResponse,
            false,
            null,
            null
        );
    }
}
