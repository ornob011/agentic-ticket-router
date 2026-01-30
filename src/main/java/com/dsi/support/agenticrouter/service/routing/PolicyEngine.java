package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PolicyEngine {

    private final PolicyConfigRepository policyConfigRepository;

    public RouterResponse applyPolicyGates(
        RouterResponse routerResponse
    ) {
        RouterResponse.RouterResponseBuilder builder = RouterResponse.builder()
                                                                     .category(routerResponse.getCategory())
                                                                     .priority(routerResponse.getPriority())
                                                                     .queue(routerResponse.getQueue())
                                                                     .nextAction(routerResponse.getNextAction())
                                                                     .confidence(routerResponse.getConfidence())
                                                                     .clarifyingQuestion(routerResponse.getClarifyingQuestion())
                                                                     .draftReply(routerResponse.getDraftReply())
                                                                     .rationaleTags(routerResponse.getRationaleTags());

        boolean policyTriggered = false;

        if (isSecurityContent(routerResponse)) {
            builder.queue(TicketQueue.SECURITY_Q);
            builder.nextAction(NextAction.ESCALATE);
            builder.priority(TicketPriority.HIGH);
            policyTriggered = true;
        }

        if (TicketPriority.CRITICAL.equals(routerResponse.getPriority())) {
            BigDecimal criticalMinConf = getConfigValue(
                "CRITICAL_MIN_CONF",
                BigDecimal.valueOf(0.85)
            );

            if (routerResponse.getConfidence().compareTo(criticalMinConf) < 0) {
                builder.nextAction(NextAction.HUMAN_REVIEW);
                policyTriggered = true;
            }
        }

        BigDecimal autoRouteThreshold = getConfigValue(
            "AUTO_ROUTE_THRESHOLD",
            BigDecimal.valueOf(0.70)
        );

        if (routerResponse.getConfidence().compareTo(autoRouteThreshold) < 0 && !policyTriggered) {
            builder.nextAction(NextAction.HUMAN_REVIEW);
        }

        return builder.build();
    }

    private boolean isSecurityContent(
        RouterResponse routerResponse
    ) {
        if (TicketCategory.SECURITY.equals(routerResponse.getCategory())) {
            return true;
        }

        return routerResponse.getRationaleTags()
                             .stream()
                             .anyMatch(
                                 tag -> SecurityTag.getDangerousTags().contains(tag.toUpperCase())
                             );
    }

    private BigDecimal getConfigValue(
        String key,
        BigDecimal defaultValue
    ) {
        return policyConfigRepository.findByConfigKeyAndActiveTrue(key)
                                     .map(PolicyConfig::getConfigValue)
                                     .map(Double::parseDouble)
                                     .map(BigDecimal::new)
                                     .orElse(defaultValue);
    }
}
