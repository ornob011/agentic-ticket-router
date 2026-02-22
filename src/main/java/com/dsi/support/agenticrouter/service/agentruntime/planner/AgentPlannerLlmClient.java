package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.dto.ArticleSearchResult;
import com.dsi.support.agenticrouter.dto.PatternHint;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.service.agentruntime.orchestration.AgentOrchestrationModeResolver;
import com.dsi.support.agenticrouter.service.ai.*;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentPlannerLlmClient {

    private static final EnumPromptValues ENUM_PROMPT_VALUES = new EnumPromptValues(
        buildEnumValues(TicketCategory.values()),
        buildEnumValues(TicketPriority.values()),
        buildEnumValues(TicketQueue.values()),
        buildEnumValues(NextAction.values())
    );

    private static final String ROUTING_POLICY = buildRoutingPolicy();

    private final ChatModel chatModel;
    private final PromptService promptService;
    private final LlmPromptCaller llmPromptCaller;
    private final LlmResponseTextExtractor llmResponseTextExtractor;
    private final LlmJsonResponseParser llmJsonResponseParser;
    private final ObjectMapper objectMapper;
    private final TokenCountService tokenCountService;
    private final AgentOrchestrationModeResolver agentOrchestrationModeResolver;

    public String requestPlan(
        RouterRequest routerRequest,
        Long ticketId,
        AgentRole actorRole
    ) {
        AgentOrchestrationMode orchestrationMode = agentOrchestrationModeResolver.mode();

        Resource plannerPromptResource = promptService.getPlannerPromptByFlowMode(
            orchestrationMode
        );

        String plannerTemplateName = orchestrationMode.plannerTemplateName();

        log.info(
            "AgentPlanner({}) SupportTicket(id:{}) Planner(mode:{},template:{},role:{}) Outcome(start)",
            OperationalLogContext.PHASE_START,
            ticketId,
            orchestrationMode,
            plannerTemplateName,
            actorRole
        );

        String latestCustomerMessage = getLatestCustomerMessage(
            routerRequest
        );

        String latestAssistantMessage = StringUtils.defaultString(
            routerRequest.getLatestAssistantMessage()
        );

        EnumPromptValues promptValues = ENUM_PROMPT_VALUES;

        String relevantArticles = formatRelevantArticles(
            routerRequest.getRelevantArticles()
        );

        String relevantPatterns = formatRelevantPatterns(
            routerRequest.getRelevantPatterns()
        );

        String routingPolicy = ROUTING_POLICY;

        String plannerRawJson = llmResponseTextExtractor.extractRequiredContent(
            llmPromptCaller.call(
                chatModel,
                promptUserSpec -> promptUserSpec
                    .text(plannerPromptResource)
                    .param("category", promptValues.category())
                    .param("priority", promptValues.priority())
                    .param("queue", promptValues.queue())
                    .param("next_action", promptValues.nextAction())
                    .param("ticket_no", routerRequest.getTicketNo())
                    .param("subject", routerRequest.getSubject())
                    .param("customer_name", routerRequest.getCustomerName())
                    .param("customer_tier", routerRequest.getCustomerTier())
                    .param("initial_message", routerRequest.getInitialMessage())
                    .param("conversation_history", routerRequest.getConversationHistory())
                    .param("latest_customer_message", latestCustomerMessage)
                    .param("latest_assistant_message", latestAssistantMessage)
                    .param("relevant_articles", relevantArticles)
                    .param("relevant_patterns", relevantPatterns)
                    .param("routing_policy", routingPolicy)
                    .param("agent_role", actorRole.name())
                    .param("agent_role_description", actorRole.getDescription())
            ),
            plannerTemplateName
        );

        log.info(
            "AgentPlanner({}) SupportTicket(id:{}) Planner(mode:{},template:{},role:{}) Outcome(rawTokensEst:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            orchestrationMode,
            plannerTemplateName,
            actorRole,
            tokenCountService.countTokens(plannerRawJson)
        );

        return plannerRawJson;
    }

    public String repairPlan(
        String plannerRawJson,
        String validationError,
        RouterRequest routerRequest,
        Long ticketId,
        AgentRole actorRole
    ) {
        AgentOrchestrationMode orchestrationMode = agentOrchestrationModeResolver.mode();

        Resource repairPromptResource = promptService.getPlannerRepairPromptByFlowMode(
            orchestrationMode
        );

        String repairTemplateName = orchestrationMode.repairTemplateName();

        EnumPromptValues promptValues = ENUM_PROMPT_VALUES;

        String normalizedPlannerRawJson = Objects.requireNonNullElse(
            plannerRawJson,
            StringUtils.EMPTY
        );

        String normalizedValidationError = normalizeValidationError(
            validationError
        );

        String routingPolicy = ROUTING_POLICY;

        String repairedJson = llmResponseTextExtractor.extractRequiredContent(
            llmPromptCaller.call(
                chatModel,
                promptUserSpec -> promptUserSpec
                    .text(repairPromptResource)
                    .param("failed_response", normalizedPlannerRawJson)
                    .param("error_message", normalizedValidationError)
                    .param("category_values", promptValues.category())
                    .param("priority_values", promptValues.priority())
                    .param("queue_values", promptValues.queue())
                    .param("next_action_values", promptValues.nextAction())
                    .param("planner_raw_json", normalizedPlannerRawJson)
                    .param("validation_error", normalizedValidationError)
                    .param("category", promptValues.category())
                    .param("priority", promptValues.priority())
                    .param("queue", promptValues.queue())
                    .param("next_action", promptValues.nextAction())
                    .param("ticket_no", routerRequest.getTicketNo())
                    .param("subject", routerRequest.getSubject())
                    .param("customer_name", routerRequest.getCustomerName())
                    .param("initial_message", routerRequest.getInitialMessage())
                    .param("routing_policy", routingPolicy)
                    .param("agent_role", actorRole.name())
            ),
            repairTemplateName
        );

        log.info(
            "AgentPlannerRepair({}) SupportTicket(id:{}) Planner(mode:{},template:{},role:{}) Outcome(repairedTokensEst:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            orchestrationMode,
            repairTemplateName,
            actorRole,
            tokenCountService.countTokens(repairedJson)
        );

        return repairedJson;
    }

    public RouterResponse mapUnchecked(
        String candidateJson
    ) {
        JsonNode jsonNode = llmJsonResponseParser.parseJsonObjectToNode(
            candidateJson
        );

        return objectMapper.convertValue(
            jsonNode,
            RouterResponse.class
        );
    }

    private String getLatestCustomerMessage(
        RouterRequest routerRequest
    ) {
        return StringUtils.defaultIfBlank(
            routerRequest.getLatestCustomerMessage(),
            StringUtils.defaultString(routerRequest.getInitialMessage())
        );
    }

    private String formatRelevantPatterns(
        List<PatternHint> patterns
    ) {
        if (CollectionUtils.isEmpty(patterns)) {
            return "No historical patterns available.";
        }

        return "HISTORICAL PATTERNS (from human feedback on similar tickets):\n" +
               patterns.stream()
                       .map(patternHint -> String.format(
                           "- Category: %s, Action: %s, Success rate: %.0f%% over %d samples",
                           patternHint.category(),
                           patternHint.successfulAction(),
                           patternHint.successRate() * 100,
                           patternHint.sampleCount()
                       ))
                       .collect(Collectors.joining("\n")) +
               "\nUse these patterns to inform your decision, but prioritize current intent signals.";
    }

    private String formatRelevantArticles(
        List<ArticleSearchResult> articles
    ) {
        if (CollectionUtils.isEmpty(articles)) {
            return "No relevant articles found.";
        }

        return articles.stream()
                       .map(article -> String.format(
                           "- Article ID: %d | Title: %s | Similarity: %.2f | Category: %s | Priority: %d",
                           article.getArticleId(),
                           article.getTitle(),
                           article.getSimilarityScore(),
                           article.getCategory(),
                           article.getPriority()
                       ))
                       .collect(Collectors.joining("\n"));
    }

    private static String buildEnumValues(Enum<?>[] values) {
        return Arrays.stream(values)
                     .map(Enum::name)
                     .collect(Collectors.joining(" | "));
    }

    private static String buildRoutingPolicy() {
        return Stream.of(TicketCategory.values())
                     .map(category -> String.format(
                         "- %s -> %s",
                         category.name(),
                         queueForCategoryStatic(category).name()
                     ))
                     .collect(Collectors.joining("\n"));
    }

    private static TicketQueue queueForCategoryStatic(TicketCategory category) {
        TicketCategory routingCategory = Objects.requireNonNullElse(
            category,
            TicketCategory.OTHER
        ).toRoutingCategory();

        return switch (routingCategory) {
            case BILLING -> TicketQueue.BILLING_Q;
            case TECHNICAL -> TicketQueue.TECH_Q;
            case SHIPPING -> TicketQueue.OPS_Q;
            case SECURITY -> TicketQueue.SECURITY_Q;
            case ACCOUNT, PRICING, CANCEL -> TicketQueue.ACCOUNT_Q;
            default -> TicketQueue.GENERAL_Q;
        };
    }

    private String normalizeValidationError(
        String validationError
    ) {
        if (StringUtils.isBlank(validationError)) {
            return "none";
        }

        return validationError;
    }

    private record EnumPromptValues(
        String category,
        String priority,
        String queue,
        String nextAction
    ) {
    }
}
