package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.ArticleSearchResult;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.service.ai.*;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoutingLlmClient implements RoutingModelClient {

    private final ChatModel chatModel;
    private final PromptService promptService;
    private final LlmPromptCaller llmPromptCaller;
    private final LlmResponseTextExtractor llmResponseTextExtractor;
    private final LlmJsonResponseParser llmJsonResponseParser;
    private final TokenCountService tokenCountService;

    @Override
    public JsonNode requestRoutingDecision(
        RouterRequest routerRequest
    ) {
        String latestCustomerMessage = getLatestCustomerMessage(
            routerRequest
        );

        String latestAssistantMessage = StringUtils.defaultString(
            routerRequest.getLatestAssistantMessage()
        );

        log.debug(
            "RoutingModelCall({}) RouterRequest(ticketId:{},ticketNo:{}) Outcome(subjectLength:{},conversationLength:{},analysisLength:{},subjectTokensEst:{},conversationTokensEst:{},analysisTokensEst:{},relevantArticleCount:{})",
            OperationalLogContext.PHASE_START,
            routerRequest.getTicketId(),
            routerRequest.getTicketNo(),
            StringUtils.length(routerRequest.getSubject()),
            StringUtils.length(routerRequest.getConversationHistory()),
            StringUtils.length(routerRequest.getAnalysis()),
            tokenCountService.countTokens(routerRequest.getSubject()),
            tokenCountService.countTokens(routerRequest.getConversationHistory()),
            tokenCountService.countTokens(routerRequest.getAnalysis()),
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

        String responseText = llmResponseTextExtractor.extractRequiredContent(
            llmPromptCaller.call(
                chatModel,
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
                    .param("latest_assistant_message", latestAssistantMessage)
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
            ),
            "routing_decision"
        );

        log.debug(
            "RoutingModelCall({}) RouterRequest(ticketId:{},ticketNo:{}) Outcome(responseLength:{},responseTokensEst:{})",
            OperationalLogContext.PHASE_COMPLETE,
            routerRequest.getTicketId(),
            routerRequest.getTicketNo(),
            StringUtils.length(responseText),
            tokenCountService.countTokens(responseText)
        );

        Objects.requireNonNull(responseText);

        return llmJsonResponseParser.parseJsonObjectToNode(
            responseText
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
