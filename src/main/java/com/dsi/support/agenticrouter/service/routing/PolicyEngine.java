package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

// TODO: Fix the class

/**
 * Applies hard business rules and confidence gating to routing decisions.
 * Policy gates override LLM output when necessary for safety and compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEngine {

    private final PolicyConfigRepository policyConfigRepository;

    /**
     * Apply policy gates to routing response.
     * May override LLM decision based on hard safety rules.
     *
     * @param response Original LLM response
     * @param ticket   Ticket being routed
     * @return Potentially modified response
     */
    public RouterResponse applyPolicyGates(RouterResponse response, SupportTicket ticket) {
        log.debug("Applying policy gates to routing decision for ticket: {}",
            ticket.getFormattedTicketNo());

        RouterResponse.RouterResponseBuilder builder = RouterResponse.builder()
                                                                     .category(response.getCategory())
                                                                     .priority(response.getPriority())
                                                                     .queue(response.getQueue())
                                                                     .nextAction(response.getNextAction())
                                                                     .confidence(response.getConfidence())
                                                                     .clarifyingQuestion(response.getClarifyingQuestion())
                                                                     .draftReply(response.getDraftReply())
                                                                     .rationaleTags(response.getRationaleTags());

        boolean policyTriggered = false;

        // HARD RULE 1: Security/PII escalation
        if (isSecurityContent(response)) {
            log.warn("Security content detected - forcing escalation");
            builder.queue(TicketQueue.SECURITY_Q);
            builder.nextAction(NextAction.ESCALATE);
            builder.priority(TicketPriority.HIGH);
            policyTriggered = true;
        }

        // HARD RULE 2: Critical tickets require high confidence
        if (response.getPriority() == TicketPriority.CRITICAL) {
            BigDecimal criticalMinConf = getConfigValue("CRITICAL_MIN_CONF", BigDecimal.valueOf(0.85));
            if (response.getConfidence().compareTo(criticalMinConf) < 0) {
                log.warn("Critical ticket with low confidence - forcing human review");
                builder.nextAction(NextAction.HUMAN_REVIEW);
                policyTriggered = true;
            }
        }

        // CONFIDENCE GATING: Low confidence -> human review
        BigDecimal autoRouteThreshold = getConfigValue("AUTO_ROUTE_THRESHOLD", BigDecimal.valueOf(0.70));
        if (response.getConfidence().compareTo(autoRouteThreshold) < 0 && !policyTriggered) {
            log.info("Confidence {} below threshold {} - routing to human review",
                response.getConfidence(), autoRouteThreshold);
            builder.nextAction(NextAction.HUMAN_REVIEW);
        }

        return builder.build();
    }

    /**
     * Check if routing indicates security-related content.
     */
    private boolean isSecurityContent(RouterResponse response) {
        // Check category
        if (response.getCategory() == TicketCategory.SECURITY) {
            return true;
        }

        // Check rationale tags
        List<String> dangerousTags = List.of("THREAT", "PII_RISK", "SECURITY_BREACH",
            "HACK", "BREACH", "VULNERABILITY");

        return response.getRationaleTags().stream()
                       .anyMatch(tag -> dangerousTags.contains(tag.toUpperCase()));
    }

    /**
     * Get configuration value with fallback.
     */
    private BigDecimal getConfigValue(String key, BigDecimal defaultValue) {
        return policyConfigRepository.findByConfigKeyAndActiveTrue(key)
                                     .map(PolicyConfig::getConfigValue)
                                     .map(BigDecimal::new)
                                     .orElse(defaultValue);
    }
}
