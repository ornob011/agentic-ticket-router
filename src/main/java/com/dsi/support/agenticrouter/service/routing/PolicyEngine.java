package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.RoutingPolicyRuleCode;
import com.dsi.support.agenticrouter.service.routing.policy.PolicyEvaluationState;
import com.dsi.support.agenticrouter.service.routing.policy.PolicyRuleOutcome;
import com.dsi.support.agenticrouter.service.routing.policy.RouterPolicyRule;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEngine {

    private final List<RouterPolicyRule> policyRules;

    public RouterResponse applyPolicyGates(
        RouterResponse routerResponse
    ) {
        log.info(
            "PolicyEvaluate({}) RouterResponse(category:{},priority:{},queue:{},nextAction:{},confidence:{})",
            OperationalLogContext.PHASE_START,
            routerResponse.getCategory(),
            routerResponse.getPriority(),
            routerResponse.getQueue(),
            routerResponse.getNextAction(),
            routerResponse.getConfidence()
        );

        PolicyEvaluationState state = new PolicyEvaluationState(
            routerResponse,
            false
        );

        for (RouterPolicyRule policyRule : policyRules) {
            PolicyRuleOutcome outcome = policyRule.apply(
                state
            );
            logPolicyDecision(
                policyRule.code(),
                outcome
            );
            state = outcome.state();
        }

        RouterResponse gatedResponse = state.response();

        log.info(
            "PolicyEvaluate({}) RouterResponse(category:{},priority:{},queue:{},nextAction:{},confidence:{})",
            OperationalLogContext.PHASE_COMPLETE,
            gatedResponse.getCategory(),
            gatedResponse.getPriority(),
            gatedResponse.getQueue(),
            gatedResponse.getNextAction(),
            gatedResponse.getConfidence()
        );

        return gatedResponse;
    }

    private void logPolicyDecision(
        RoutingPolicyRuleCode ruleCode,
        PolicyRuleOutcome outcome
    ) {
        if (!outcome.changed()) {
            return;
        }

        RouterResponse response = outcome.state().response();

        log.warn(
            "PolicyEvaluate({}) Outcome(policy:{},nextAction:{},queue:{},priority:{},confidence:{},policyTriggered:{})",
            OperationalLogContext.PHASE_DECISION,
            ruleCode.code(),
            response.getNextAction(),
            response.getQueue(),
            response.getPriority(),
            response.getConfidence(),
            outcome.state().policyTriggered()
        );
    }
}
