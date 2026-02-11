package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.ArticleSearchResult;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.ModelRegistryRepository;
import com.dsi.support.agenticrouter.service.ai.LlmOutputService;
import com.dsi.support.agenticrouter.service.ai.PromptService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaRouterService {

    private static final String KEY_TEMPLATE_ID = "template_id";

    private final OllamaChatModel ollamaChatModel;
    private final ModelRegistryRepository modelRegistryRepository;
    private final LlmOutputService llmOutputService;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    @Retry(
        name = "ollamaRetry",
        fallbackMethod = "routingFallback"
    )
    @CircuitBreaker(
        name = "ollamaCircuit",
        fallbackMethod = "routingFallback"
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RouterResponse getRoutingDecision(
        RouterRequest routerRequest,
        Long ticketId
    ) {
        log.info(
            "RoutingDecision({}) SupportTicket(id:{},ticketNo:{}) RouterRequest(subjectLength:{},conversationLength:{},analysisLength:{},relevantArticleCount:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            routerRequest.getTicketNo(),
            StringUtils.length(routerRequest.getSubject()),
            StringUtils.length(routerRequest.getConversationHistory()),
            StringUtils.length(routerRequest.getAnalysis()),
            CollectionUtils.size(routerRequest.getRelevantArticles())
        );

        ModelRegistry activeModel = modelRegistryRepository.findByActiveTrue()
                                                           .stream()
                                                           .findFirst()
                                                           .orElseThrow(
                                                               DataNotFoundException.supplier(
                                                                   ModelRegistry.class,
                                                                   "active model"
                                                               )
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

        ChatClient chatClient = ChatClient.builder(ollamaChatModel)
                                          .build();

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
            "RoutingDecision({}) SupportTicket(id:{}) Model(tag:{}) Outcome(responseLength:{})",
            OperationalLogContext.PHASE_DECISION,
            ticketId,
            activeModel.getModelTag(),
            StringUtils.length(responseText)
        );

        RouterResponse routerResponse;
        try {
            routerResponse = objectMapper.readValue(
                responseText,
                RouterResponse.class
            );
        } catch (JsonProcessingException exception) {
            log.error(
                "RoutingDecision({}) SupportTicket(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                ticketId,
                "response_json_parse_failed",
                exception
            );

            throw new IllegalStateException("Failed to parse router response JSON", exception);
        }

        validateRouterResponse(
            routerResponse
        );

        llmOutputService.persistRoutingOutput(
            ticketId,
            activeModel.getModelTag(),
            promptService.getRoutingPrompt().toString(),
            routerResponse,
            ParseStatus.SUCCESS,
            null,
            0
        );

        log.info(
            "RoutingDecision({}) SupportTicket(id:{}) Model(tag:{}) RouterResponse(category:{},priority:{},queue:{},nextAction:{},confidence:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            activeModel.getModelTag(),
            routerResponse.getCategory(),
            routerResponse.getPriority(),
            routerResponse.getQueue(),
            routerResponse.getNextAction(),
            routerResponse.getConfidence()
        );

        return routerResponse;
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

    private void validateRouterResponse(
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

        if (routerResponse.getConfidence().compareTo(BigDecimal.ZERO) < 0
            || routerResponse.getConfidence().compareTo(BigDecimal.ONE) > 0
        ) {
            throw new IllegalStateException("Confidence must be between 0 and 1");
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
                                        .map(params -> params.get(KEY_TEMPLATE_ID))
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

    public RouterResponse routingFallback(
        RouterRequest routerRequest,
        Long ticketId,
        Throwable throwable
    ) {
        log.error(
            "RoutingFallback({}) SupportTicket(id:{}) RouterRequest(ticketNo:{}) Outcome(errorType:{},message:{})",
            OperationalLogContext.PHASE_FAIL,
            ticketId,
            routerRequest.getTicketNo(),
            throwable.getClass().getSimpleName(),
            throwable.getMessage(),
            throwable
        );

        llmOutputService.persistRoutingOutputInNewTransaction(
            ticketId,
            "fallback",
            routerRequest.toString(),
            null,
            ParseStatus.TIMEOUT,
            throwable.getMessage(),
            0
        );

        RouterResponse fallbackResponse = RouterResponse.builder()
                                                        .category(TicketCategory.OTHER)
                                                        .priority(TicketPriority.MEDIUM)
                                                        .queue(TicketQueue.GENERAL_Q)
                                                        .nextAction(NextAction.HUMAN_REVIEW)
                                                        .confidence(BigDecimal.ZERO)
                                                        .clarifyingQuestion(null)
                                                        .draftReply(null)
                                                        .rationaleTags(
                                                            Collections.singletonList(
                                                                String.format(
                                                                    "MODEL_ERROR:%s",
                                                                    throwable.getMessage()
                                                                )
                                                            )
                                                        )
                                                        .build();

        log.warn(
            "RoutingFallback({}) SupportTicket(id:{}) RouterResponse(category:{},priority:{},queue:{},nextAction:{},confidence:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            fallbackResponse.getCategory(),
            fallbackResponse.getPriority(),
            fallbackResponse.getQueue(),
            fallbackResponse.getNextAction(),
            fallbackResponse.getConfidence()
        );

        return fallbackResponse;
    }
}
