package com.dsi.support.agenticrouter.service.routing.policy;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@Order(1)
@RequiredArgsConstructor
public class SecurityContentEscalationRule implements RouterPolicyRule {

    private final RouterResponseEditor routerResponseEditor;

    @Override
    public RoutingPolicyRuleCode code() {
        return RoutingPolicyRuleCode.SECURITY_CONTENT_ESCALATION;
    }

    @Override
    public PolicyRuleOutcome apply(
        PolicyEvaluationState state
    ) {
        RouterResponse response = state.response();

        if (!isSecurityContent(response)) {
            return PolicyRuleOutcome.unchanged(state);
        }

        RouterResponse updatedResponse = routerResponseEditor.mutate(
            response,
            builder -> builder.queue(TicketQueue.SECURITY_Q)
                              .nextAction(NextAction.ESCALATE)
                              .priority(TicketPriority.HIGH)
        );

        return PolicyRuleOutcome.changed(
            state.withResponseAndTriggered(
                updatedResponse
            )
        );
    }

    private boolean isSecurityContent(
        RouterResponse routerResponse
    ) {
        if (TicketCategory.SECURITY.equals(routerResponse.getCategory())) {
            return true;
        }

        List<String> rationaleTags = Objects.requireNonNullElse(
            routerResponse.getRationaleTags(),
            List.of()
        );

        return rationaleTags.stream()
                            .map(StringNormalizationUtils::upperTrimmedOrEmpty)
                            .anyMatch(SecurityTag::isDangerous);
    }
}
