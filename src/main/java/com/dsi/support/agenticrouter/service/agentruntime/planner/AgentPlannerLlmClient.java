package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.dto.ArticleSearchResult;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.service.agentruntime.orchestration.AgentOrchestrationModeResolver;
import com.dsi.support.agenticrouter.service.ai.*;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
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

        EnumPromptValues promptValues = enumPromptValues();

        String relevantArticles = formatRelevantArticles(
            routerRequest.getRelevantArticles()
        );

        String routingPolicy = routingPolicy();

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
                    .param("analysis", routerRequest.getAnalysis())
                    .param("latest_customer_message", latestCustomerMessage)
                    .param("relevant_articles", relevantArticles)
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

        EnumPromptValues promptValues = enumPromptValues();

        String normalizedPlannerRawJson = Objects.requireNonNullElse(
            plannerRawJson,
            StringUtils.EMPTY
        );

        String normalizedValidationError = normalizeValidationError(
            validationError
        );

        String routingPolicy = routingPolicy();

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
        String conversationHistory = StringUtils.defaultString(
            routerRequest.getConversationHistory()
        );

        String[] historyLines = StringUtils.splitPreserveAllTokens(
            conversationHistory,
            '\n'
        );

        if (Objects.isNull(historyLines) || historyLines.length == 0) {
            return StringNormalizationUtils.trimToEmpty(routerRequest.getInitialMessage());
        }

        List<String> lines = Arrays.stream(historyLines).toList();

        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (Objects.isNull(line) || !line.contains("CUSTOMER_MESSAGE:")) {
                continue;
            }

            String extracted = StringNormalizationUtils.trimToEmpty(
                StringUtils.substringAfter(line, "CUSTOMER_MESSAGE:")
            );

            if (StringUtils.isNotBlank(extracted)) {
                return extracted;
            }
        }

        return StringNormalizationUtils.trimToEmpty(routerRequest.getInitialMessage());
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

    private EnumPromptValues enumPromptValues() {
        return new EnumPromptValues(
            enumValues(TicketCategory.values()),
            enumValues(TicketPriority.values()),
            enumValues(TicketQueue.values()),
            enumValues(NextAction.values())
        );
    }

    private String enumValues(
        Enum<?>[] values
    ) {
        return Arrays.stream(values)
                     .map(Enum::name)
                     .collect(Collectors.joining(" | "));
    }

    private String normalizeValidationError(
        String validationError
    ) {
        if (StringUtils.isBlank(validationError)) {
            return "none";
        }

        return validationError;
    }

    private String routingPolicy() {
        return Stream.of(TicketCategory.values())
                     .map(category -> String.format(
                         "- %s -> %s",
                         category.name(),
                         queueForCategory(category).name()
                     ))
                     .collect(Collectors.joining("\n"));
    }

    private TicketQueue queueForCategory(
        TicketCategory category
    ) {
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

    private record EnumPromptValues(
        String category,
        String priority,
        String queue,
        String nextAction
    ) {
    }
}
