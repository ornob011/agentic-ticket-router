package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.RoutingActionParameterKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class RouterResponseContractValidator {

    public void validate(
        RouterResponse routerResponse
    ) {
        Objects.requireNonNull(routerResponse, "response is required");
        Objects.requireNonNull(routerResponse.getCategory(), "category is required");
        Objects.requireNonNull(routerResponse.getPriority(), "priority is required");
        Objects.requireNonNull(routerResponse.getQueue(), "queue is required");
        Objects.requireNonNull(routerResponse.getNextAction(), "next_action is required");
        Objects.requireNonNull(routerResponse.getConfidence(), "confidence is required");

        validateTemplateId(
            routerResponse
        );
        validateKnowledgeArticleId(
            routerResponse
        );
        validateProfileUpdateParameters(
            routerResponse
        );
        validateAssignQueueContracts(
            routerResponse
        );
        validateAskClarifyingContracts(
            routerResponse
        );

        if (routerResponse.getConfidence().compareTo(BigDecimal.ZERO) < 0
            || routerResponse.getConfidence().compareTo(BigDecimal.ONE) > 0
        ) {
            throw new IllegalStateException("Confidence must be between 0 and 1");
        }
    }

    private void validateAssignQueueContracts(
        RouterResponse routerResponse
    ) {
        if (!NextAction.ASSIGN_QUEUE.equals(routerResponse.getNextAction())) {
            return;
        }

        if (StringUtils.isNotBlank(routerResponse.getClarifyingQuestion())) {
            throw new IllegalStateException("clarifying_question must be null for ASSIGN_QUEUE");
        }

        if (StringUtils.isNotBlank(routerResponse.getDraftReply())) {
            throw new IllegalStateException("draft_reply must be null for ASSIGN_QUEUE");
        }
    }

    private void validateAskClarifyingContracts(
        RouterResponse routerResponse
    ) {
        if (!NextAction.ASK_CLARIFYING.equals(routerResponse.getNextAction())) {
            return;
        }

        if (StringUtils.isBlank(routerResponse.getClarifyingQuestion())) {
            throw new IllegalStateException("clarifying_question is required for ASK_CLARIFYING");
        }
    }

    private void validateKnowledgeArticleId(
        RouterResponse routerResponse
    ) {
        if (!NextAction.USE_KNOWLEDGE_ARTICLE.equals(routerResponse.getNextAction())) {
            return;
        }

        Map<String, ?> actionParameters = routerResponse.getActionParameters();
        String articleIdText = Optional.ofNullable(actionParameters)
                                       .map(params -> params.get(RoutingActionParameterKey.ARTICLE_ID.getKey()))
                                       .map(Object::toString)
                                       .map(StringUtils::trimToNull)
                                       .orElse(null);

        if (StringUtils.isBlank(articleIdText) || !StringUtils.isNumeric(articleIdText)) {
            throw new IllegalStateException("article_id must be numeric for USE_KNOWLEDGE_ARTICLE");
        }

        if (Long.parseLong(articleIdText) <= 0L) {
            throw new IllegalStateException("article_id must be positive for USE_KNOWLEDGE_ARTICLE");
        }
    }

    private void validateProfileUpdateParameters(
        RouterResponse routerResponse
    ) {
        if (!NextAction.UPDATE_CUSTOMER_PROFILE.equals(routerResponse.getNextAction())) {
            return;
        }

        Map<String, ?> actionParameters = Optional.ofNullable(routerResponse.getActionParameters())
                                                  .orElseThrow(() -> new IllegalStateException("action_parameters is required for UPDATE_CUSTOMER_PROFILE"));

        boolean hasAnyProfileValue = Stream.of(
                                               RoutingActionParameterKey.PHONE_NUMBER,
                                               RoutingActionParameterKey.COMPANY_NAME,
                                               RoutingActionParameterKey.ADDRESS,
                                               RoutingActionParameterKey.CITY,
                                               RoutingActionParameterKey.POSTAL_CODE,
                                               RoutingActionParameterKey.PREFERRED_LANGUAGE_CODE
                                           )
                                           .map(RoutingActionParameterKey::getKey)
                                           .map(actionParameters::get)
                                           .map(value -> Objects.toString(value, null))
                                           .map(StringUtils::trimToNull)
                                           .anyMatch(StringUtils::isNotBlank);

        if (!hasAnyProfileValue) {
            throw new IllegalStateException("at least one profile field is required for UPDATE_CUSTOMER_PROFILE");
        }
    }

    private void validateTemplateId(
        RouterResponse routerResponse
    ) {
        if (!NextAction.USE_TEMPLATE.equals(routerResponse.getNextAction())) {
            return;
        }

        Map<String, ?> actionParameters = routerResponse.getActionParameters();

        String templateIdText = Optional.ofNullable(actionParameters)
                                        .map(params -> params.get(RoutingActionParameterKey.TEMPLATE_ID.getKey()))
                                        .map(Object::toString)
                                        .map(StringUtils::trimToNull)
                                        .orElse(null);

        if (StringUtils.isBlank(templateIdText)) {
            return;
        }

        if (!StringUtils.isNumeric(templateIdText)) {
            throw new IllegalStateException("template_id must be numeric");
        }

        long templateId = Long.parseLong(templateIdText);

        if (templateId <= 0L) {
            throw new IllegalStateException("template_id must be positive");
        }
    }
}
