package com.dsi.support.agenticrouter.service.agentruntime.planner;

import com.dsi.support.agenticrouter.dto.ArticleSearchResult;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.ai.ChatClientFactory;
import com.dsi.support.agenticrouter.service.ai.PromptService;
import com.dsi.support.agenticrouter.service.ai.TokenCountService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.MarkdownCodeBlockCleaner;
import org.springframework.ai.converter.ResponseTextCleaner;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentPlannerLlmClient {

    private static final ResponseTextCleaner RESPONSE_TEXT_CLEANER = new MarkdownCodeBlockCleaner();
    private static final JacksonJsonParser JSON_PARSER = new JacksonJsonParser();

    private final ChatModel chatModel;
    private final PromptService promptService;
    private final ChatClientFactory chatClientFactory;
    private final ObjectMapper objectMapper;
    private final TokenCountService tokenCountService;

    public String requestPlan(
        RouterRequest routerRequest,
        Long ticketId
    ) {
        String latestCustomerMessage = getLatestCustomerMessage(
            routerRequest
        );

        EnumPromptValues promptValues = enumPromptValues();

        String relevantArticles = formatRelevantArticles(
            routerRequest.getRelevantArticles()
        );

        ChatClient.ChatClientRequestSpec requestSpec = chatClientFactory.create(chatModel)
                                                                        .prompt()
                                                                        .system(promptService.getSystemPrompt())
                                                                        .user(promptUserSpec -> promptUserSpec
                                                                            .text(promptService.getAgentPlannerPrompt())
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
                                                                        );

        String plannerRawJson = requestSpec.call()
                                           .content();

        log.info(
            "AgentPlanner({}) SupportTicket(id:{}) Outcome(rawTokensEst:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            tokenCountService.countTokens(plannerRawJson)
        );

        return RESPONSE_TEXT_CLEANER.clean(
            Objects.requireNonNull(plannerRawJson)
        );
    }

    public String repairPlan(
        String plannerRawJson,
        String validationError,
        RouterRequest routerRequest,
        Long ticketId
    ) {
        EnumPromptValues promptValues = enumPromptValues();

        String normalizedPlannerRawJson = Objects.requireNonNullElse(
            plannerRawJson,
            StringUtils.EMPTY
        );

        String normalizedValidationError = normalizeValidationError(
            validationError
        );

        ChatClient.ChatClientRequestSpec requestSpec = chatClientFactory.create(chatModel)
                                                                        .prompt()
                                                                        .system(promptService.getSystemPrompt())
                                                                        .user(promptUserSpec -> promptUserSpec
                                                                            .text(promptService.getAgentRepairPrompt())
                                                                            .param("planner_raw_json", normalizedPlannerRawJson)
                                                                            .param("validation_error", normalizedValidationError)
                                                                            .param("category", promptValues.category())
                                                                            .param("priority", promptValues.priority())
                                                                            .param("queue", promptValues.queue())
                                                                            .param("next_action", promptValues.nextAction())
                                                                            .param("ticket_no", routerRequest.getTicketNo())
                                                                        );

        String repairedJson = requestSpec.call()
                                         .content();

        log.info(
            "AgentPlannerRepair({}) SupportTicket(id:{}) Outcome(repairedTokensEst:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            tokenCountService.countTokens(repairedJson)
        );

        return RESPONSE_TEXT_CLEANER.clean(
            Objects.requireNonNull(repairedJson)
        );
    }

    public RouterResponse mapUnchecked(
        String candidateJson
    ) {
        JsonNode jsonNode = parseJsonNode(
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

    private JsonNode parseJsonNode(
        String rawPlannerJson
    ) {
        String normalizedPlannerJson = Objects.requireNonNullElse(
            rawPlannerJson,
            "{}"
        );

        Object parsedJsonObject = JSON_PARSER.parseMap(
            RESPONSE_TEXT_CLEANER.clean(normalizedPlannerJson)
        );

        return objectMapper.valueToTree(parsedJsonObject);
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

    private record EnumPromptValues(
        String category,
        String priority,
        String queue,
        String nextAction
    ) {
    }
}
