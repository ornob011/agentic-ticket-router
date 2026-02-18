package com.dsi.support.agenticrouter.service.agentruntime.safety;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.AgentPolicyReason;
import com.dsi.support.agenticrouter.enums.AgentSafetyDecisionStatus;
import com.dsi.support.agenticrouter.service.routing.PolicyEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AgentSafetyEvaluator {

    private final PolicyEngine policyEngine;

    public AgentSafetyDecision evaluate(
        RouterResponse routerResponse
    ) {
        RouterResponse safeResponse = policyEngine.applyPolicyGates(
            routerResponse
        );
        List<String> policyReasons = new ArrayList<>();

        collectOverrideReasons(
            routerResponse,
            safeResponse,
            policyReasons
        );
        boolean policyOverridden = !policyReasons.isEmpty();

        AgentSafetyDecisionStatus status = AgentSafetyDecisionStatus.ALLOW;
        if (Objects.nonNull(safeResponse.getNextAction()) && safeResponse.getNextAction().requiresHumanIntervention()) {
            status = AgentSafetyDecisionStatus.REQUIRES_HUMAN_REVIEW;
            policyReasons.add(AgentPolicyReason.HUMAN_INTERVENTION_REQUIRED.code());
        }

        if (Objects.nonNull(safeResponse.getCategory()) && safeResponse.getCategory().isHighRisk()) {
            status = AgentSafetyDecisionStatus.REQUIRES_HUMAN_REVIEW;
            policyReasons.add(AgentPolicyReason.HIGH_RISK_CATEGORY.code());
        }

        return new AgentSafetyDecision(
            status,
            safeResponse,
            policyOverridden,
            List.copyOf(policyReasons)
        );
    }

    private void collectOverrideReasons(
        RouterResponse sourceResponse,
        RouterResponse safeResponse,
        List<String> policyReasons
    ) {
        addReasonIfChanged(
            sourceResponse.getNextAction(),
            safeResponse.getNextAction(),
            AgentPolicyReason.NEXT_ACTION_OVERRIDDEN,
            policyReasons
        );
        addReasonIfChanged(
            sourceResponse.getQueue(),
            safeResponse.getQueue(),
            AgentPolicyReason.QUEUE_OVERRIDDEN,
            policyReasons
        );
        addReasonIfChanged(
            sourceResponse.getPriority(),
            safeResponse.getPriority(),
            AgentPolicyReason.PRIORITY_OVERRIDDEN,
            policyReasons
        );
        addReasonIfChanged(
            sourceResponse.getCategory(),
            safeResponse.getCategory(),
            AgentPolicyReason.CATEGORY_OVERRIDDEN,
            policyReasons
        );
    }

    private void addReasonIfChanged(
        Object before,
        Object after,
        AgentPolicyReason reason,
        List<String> policyReasons
    ) {
        if (!Objects.equals(before, after)) {
            policyReasons.add(reason.code());
        }
    }
}
