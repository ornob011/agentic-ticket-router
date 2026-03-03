package com.dsi.support.agenticrouter.service.routing.policy;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.RoutingPolicyRuleCode;
import com.dsi.support.agenticrouter.service.policy.PolicyValueLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(3)
@RequiredArgsConstructor
public class AutoRouteThresholdRule implements RouterPolicyRule {

    private final PolicyValueLookupService policyValueLookupService;
    private final RouterResponseEditor routerResponseEditor;

    @Override
    public RoutingPolicyRuleCode code() {
        return RoutingPolicyRuleCode.AUTO_ROUTE_THRESHOLD;
    }

    @Override
    public PolicyRuleOutcome apply(
        PolicyEvaluationState state
    ) {
        if (state.policyTriggered()) {
            return PolicyRuleOutcome.unchanged(state);
        }

        RouterResponse response = state.response();

        BigDecimal autoRouteThreshold = policyValueLookupService.getRequiredBigDecimalValue(
            PolicyConfigKey.AUTO_ROUTE_THRESHOLD
        );

        if (response.getConfidence().compareTo(autoRouteThreshold) >= 0) {
            return PolicyRuleOutcome.unchanged(state);
        }

        RouterResponse updatedResponse = routerResponseEditor.mutate(
            response,
            builder -> builder.nextAction(NextAction.HUMAN_REVIEW)
        );

        return PolicyRuleOutcome.changed(
            state.withResponse(
                updatedResponse
            )
        );
    }
}
