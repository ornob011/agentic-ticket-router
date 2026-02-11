package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEngine {

    private final PolicyConfigRepository policyConfigRepository;

    public RouterResponse applyPolicyGates(
        RouterResponse routerResponse
    ) {
        log.info(
            "PolicyEvaluate(start) RouterResponse(category:{},priority:{},queue:{},nextAction:{},confidence:{})",
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
                "PolicyEvaluate(decision) Outcome(policy:{},nextAction:{},queue:{},priority:{})",
                "security_content_escalation",
                NextAction.ESCALATE,
                TicketQueue.SECURITY_Q,
                TicketPriority.HIGH
            );
        }

        if (TicketPriority.CRITICAL.equals(routerResponse.getPriority())) {
            BigDecimal configValue = policyConfigRepository.findByConfigKeyAndActiveTrue(PolicyConfigKey.CRITICAL_MIN_CONF)
                                                           .map(PolicyConfig::getConfigValue)
                                                           .orElseThrow(DataNotFoundException::new);

            BigDecimal criticalMinConf = PolicyConfigKey.getBigDecimalValue(
                configValue,
                BigDecimal.valueOf(0.85)
            );

            if (routerResponse.getConfidence().compareTo(criticalMinConf) < 0) {
                builder.nextAction(NextAction.HUMAN_REVIEW);
                policyTriggered = true;

                log.warn(
                    "PolicyEvaluate(decision) Outcome(policy:{},confidence:{},threshold:{},nextAction:{})",
                    "critical_min_conf",
                    routerResponse.getConfidence(),
                    criticalMinConf,
                    NextAction.HUMAN_REVIEW
                );
            }
        }

        BigDecimal configValue = policyConfigRepository.findByConfigKeyAndActiveTrue(PolicyConfigKey.AUTO_ROUTE_THRESHOLD)
                                                       .map(PolicyConfig::getConfigValue)
                                                       .orElseThrow(DataNotFoundException::new);

        BigDecimal autoRouteThreshold = PolicyConfigKey.getBigDecimalValue(
            configValue,
            BigDecimal.valueOf(0.70)
        );

        if (routerResponse.getConfidence().compareTo(autoRouteThreshold) < 0 && !policyTriggered) {
            builder.nextAction(NextAction.HUMAN_REVIEW);

            log.info(
                "PolicyEvaluate(decision) Outcome(policy:{},confidence:{},threshold:{},nextAction:{})",
                "auto_route_threshold",
                routerResponse.getConfidence(),
                autoRouteThreshold,
                NextAction.HUMAN_REVIEW
            );
        }

        RouterResponse gatedResponse = builder.build();

        log.info(
            "PolicyEvaluate(complete) RouterResponse(category:{},priority:{},queue:{},nextAction:{},confidence:{})",
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

        return routerResponse.getRationaleTags()
                             .stream()
                             .anyMatch(
                                 tag -> SecurityTag.getDangerousTags().contains(tag.toUpperCase())
                             );
    }
}
