package com.dsi.support.agenticrouter.service.routing.policy;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.RoutingPolicyRuleCode;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.service.policy.PolicyValueLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(2)
@RequiredArgsConstructor
public class CriticalMinConfidenceRule implements RouterPolicyRule {

    private final PolicyValueLookupService policyValueLookupService;
    private final RouterResponseEditor routerResponseEditor;

    @Override
    public RoutingPolicyRuleCode code() {
        return RoutingPolicyRuleCode.CRITICAL_MIN_CONF;
    }

    @Override
    public PolicyRuleOutcome apply(
        PolicyEvaluationState state
    ) {
        RouterResponse response = state.response();

        if (!TicketPriority.CRITICAL.equals(response.getPriority())) {
            return PolicyRuleOutcome.unchanged(state);
        }

        BigDecimal criticalMinConfidence = policyValueLookupService.getRequiredBigDecimalValue(
            PolicyConfigKey.CRITICAL_MIN_CONF
        );

        if (response.getConfidence().compareTo(criticalMinConfidence) >= 0) {
            return PolicyRuleOutcome.unchanged(state);
        }

        RouterResponse updatedResponse = routerResponseEditor.mutate(
            response,
            builder -> builder.nextAction(NextAction.HUMAN_REVIEW)
        );

        return PolicyRuleOutcome.changed(
            state.withResponseAndTriggered(
                updatedResponse
            )
        );
    }
}
