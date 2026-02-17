package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.ArticleSearchResult;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.ai.ChatClientFactory;
import com.dsi.support.agenticrouter.service.ai.PromptService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoutingLlmClient implements RoutingModelClient {

    private static final ResponseTextCleaner RESPONSE_TEXT_CLEANER = new MarkdownCodeBlockCleaner();
    private final ChatModel chatModel;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final ChatClientFactory chatClientFactory;

    @Override
    public JsonNode requestRoutingDecision(
        RouterRequest routerRequest
    ) {
        log.debug(
            "RoutingModelCall({}) RouterRequest(ticketId:{},ticketNo:{}) Outcome(subjectLength:{},conversationLength:{},analysisLength:{},relevantArticleCount:{})",
            OperationalLogContext.PHASE_START,
            routerRequest.getTicketId(),
            routerRequest.getTicketNo(),
            StringUtils.length(routerRequest.getSubject()),
            StringUtils.length(routerRequest.getConversationHistory()),
            StringUtils.length(routerRequest.getAnalysis()),
            CollectionUtils.size(routerRequest.getRelevantArticles())
        );

        String categoryValues = Arrays.stream(TicketCategory.values())
                                      .map(Enum::name)
                                      .collect(Collectors.joining(" | "));

        String priorityValues = Arrays.stream(TicketPriority.values())
                                      .map(Enum::name)
                                      .collect(Collectors.joining(" | "));

        String queueValues = Arrays.stream(TicketQueue.values())
                                   .map(Enum::name)
                                   .collect(Collectors.joining(" | "));

        String nextActionValues = Arrays.stream(NextAction.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(" | "));

        String relevantArticles = formatRelevantArticles(
            routerRequest.getRelevantArticles()
        );
        String latestCustomerMessage = getLatestCustomerMessage(
            routerRequest
        );

        ChatClient chatClient = chatClientFactory.create(
            chatModel
        );

        String responseText = chatClient.prompt()
                                        .system(promptService.getSystemPrompt())
                                        .user(
                                            promptUserSpec -> promptUserSpec
                                                .text(promptService.getRoutingPrompt())
                                                .param("category", categoryValues)
                                                .param("priority", priorityValues)
                                                .param("queue", queueValues)
                                                .param("next_action", nextActionValues)
                                                .param("suggested_category", Objects.requireNonNullElse(routerRequest.getSuggestedCategory(), "None"))
                                                .param("ticket_no", routerRequest.getTicketNo())
                                                .param("subject", routerRequest.getSubject())
                                                .param("customer_name", routerRequest.getCustomerName())
                                                .param("customer_tier", routerRequest.getCustomerTier())
                                                .param("initial_message", routerRequest.getInitialMessage())
                                                .param("conversation_history", routerRequest.getConversationHistory())
                                                .param("analysis", routerRequest.getAnalysis())
                                                .param("latest_customer_message", latestCustomerMessage)
                                                .param("previous_clarifying_question",
                                                    Objects.requireNonNullElse(
                                                        routerRequest.getPreviousClarifyingQuestion(),
                                                        "None"
                                                    )
                                                )
                                                .param("relevant_articles", relevantArticles)
                                                .param("remaining_actions",
                                                    Objects.requireNonNullElse(
                                                        routerRequest.getRemainingActions(),
                                                        5
                                                    ).toString()
                                                )
                                                .param("questions_asked",
                                                    Objects.requireNonNullElse(
                                                        routerRequest.getQuestionsAsked(),
                                                        0
                                                    ).toString()
                                                )
                                                .param("max_questions",
                                                    Objects.requireNonNullElse(
                                                        routerRequest.getMaxQuestions(),
                                                        3
                                                    ).toString()
                                                )
                                                .param("max_actions",
                                                    Objects.requireNonNullElse(
                                                        routerRequest.getMaxActions(),
                                                        5
                                                    ).toString()
                                                )
                                        )
                                        .call()
                                        .content();

        log.debug(
            "RoutingModelCall({}) RouterRequest(ticketId:{},ticketNo:{}) Outcome(responseLength:{})",
            OperationalLogContext.PHASE_COMPLETE,
            routerRequest.getTicketId(),
            routerRequest.getTicketNo(),
            StringUtils.length(responseText)
        );

        Objects.requireNonNull(responseText);

        try {
            return objectMapper.readTree(
                RESPONSE_TEXT_CLEANER.clean(responseText)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse router response JSON", exception);
        }
    }

    private String getLatestCustomerMessage(
        RouterRequest routerRequest
    ) {
        String conversationHistory = StringUtils.defaultString(routerRequest.getConversationHistory());
        String[] historyLines = StringUtils.splitPreserveAllTokens(conversationHistory, '\n');

        if (historyLines == null || historyLines.length == 0) {
            return StringNormalizationUtils.trimToEmpty(routerRequest.getInitialMessage());
        }

        List<String> lines = Arrays.stream(historyLines).toList();

        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line == null || !line.contains("CUSTOMER_MESSAGE:")) {
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
}
