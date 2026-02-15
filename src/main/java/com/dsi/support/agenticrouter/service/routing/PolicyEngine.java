package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.SecurityTag;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.policy.PolicyValueLookupService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEngine {

    private final PolicyValueLookupService policyValueLookupService;

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

        RouterResponse.RouterResponseBuilder builder = RouterResponse.builder()
                                                                     .category(routerResponse.getCategory())
                                                                     .priority(routerResponse.getPriority())
                                                                     .queue(routerResponse.getQueue())
                                                                     .nextAction(routerResponse.getNextAction())
                                                                     .confidence(routerResponse.getConfidence())
                                                                     .clarifyingQuestion(routerResponse.getClarifyingQuestion())
                                                                     .draftReply(routerResponse.getDraftReply())
                                                                     .rationaleTags(routerResponse.getRationaleTags())
                                                                     .actionParameters(routerResponse.getActionParameters());

        boolean policyTriggered = false;

        if (isSecurityContent(routerResponse)) {
            builder.queue(TicketQueue.SECURITY_Q);
            builder.nextAction(NextAction.ESCALATE);
            builder.priority(TicketPriority.HIGH);
            policyTriggered = true;

            log.warn(
                "PolicyEvaluate({}) Outcome(policy:{},nextAction:{},queue:{},priority:{})",
                OperationalLogContext.PHASE_DECISION,
                "security_content_escalation",
                NextAction.ESCALATE,
                TicketQueue.SECURITY_Q,
                TicketPriority.HIGH
            );
        }

        if (TicketPriority.CRITICAL.equals(routerResponse.getPriority())) {
            BigDecimal criticalMinConf = policyValueLookupService.getRequiredBigDecimalValue(
                PolicyConfigKey.CRITICAL_MIN_CONF
            );

            if (routerResponse.getConfidence().compareTo(criticalMinConf) < 0) {
                builder.nextAction(NextAction.HUMAN_REVIEW);
                policyTriggered = true;

                log.warn(
                    "PolicyEvaluate({}) Outcome(policy:{},confidence:{},threshold:{},nextAction:{})",
                    OperationalLogContext.PHASE_DECISION,
                    "critical_min_conf",
                    routerResponse.getConfidence(),
                    criticalMinConf,
                    NextAction.HUMAN_REVIEW
                );
            }
        }

        BigDecimal autoRouteThreshold = policyValueLookupService.getRequiredBigDecimalValue(
            PolicyConfigKey.AUTO_ROUTE_THRESHOLD
        );

        if (routerResponse.getConfidence().compareTo(autoRouteThreshold) < 0 && !policyTriggered) {
            builder.nextAction(NextAction.HUMAN_REVIEW);

            log.info(
                "PolicyEvaluate({}) Outcome(policy:{},confidence:{},threshold:{},nextAction:{})",
                OperationalLogContext.PHASE_DECISION,
                "auto_route_threshold",
                routerResponse.getConfidence(),
                autoRouteThreshold,
                NextAction.HUMAN_REVIEW
            );
        }

        RouterResponse gatedResponse = builder.build();

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

    private boolean isSecurityContent(
        RouterResponse routerResponse
    ) {
        if (TicketCategory.SECURITY.equals(routerResponse.getCategory())) {
            return true;
        }

        return Optional.ofNullable(routerResponse.getRationaleTags())
                       .orElse(Collections.emptyList())
                       .stream()
                       .map(StringNormalizationUtils::upperTrimmedOrEmpty)
                       .anyMatch(SecurityTag.getDangerousTags()::contains);
    }
}
