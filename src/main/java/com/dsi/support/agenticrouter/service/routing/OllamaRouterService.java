package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.ModelRegistryRepository;
import com.dsi.support.agenticrouter.service.LlmOutputService;
import com.dsi.support.agenticrouter.service.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OllamaRouterService {

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
    public RouterResponse getRoutingDecision(
        RouterRequest routerRequest,
        Long ticketId
    ) {
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

        ChatClient chatClient = ChatClient.builder(ollamaChatModel)
                                          .defaultOptions(
                                              OllamaChatOptions.builder()
                                                               .format("json")
                                                               .build()
                                          )
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
                                                .param("ticket_no", routerRequest.getTicketNo())
                                                .param("subject", routerRequest.getSubject())
                                                .param("customer_name", routerRequest.getCustomerName())
                                                .param("customer_tier", routerRequest.getCustomerTier())
                                                .param("initial_message", routerRequest.getInitialMessage())
                                                .param("conversation_history", routerRequest.getConversationHistory())
                                                .param("analysis", routerRequest.getAnalysis())
                                        )
                                        .call()
                                        .content();

        RouterResponse routerResponse;
        try {
            routerResponse = objectMapper.readValue(
                responseText,
                RouterResponse.class
            );
        } catch (JsonProcessingException exception) {
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

        return routerResponse;
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

        if (routerResponse.getConfidence().compareTo(BigDecimal.ZERO) < 0
            || routerResponse.getConfidence().compareTo(BigDecimal.ONE) > 0
        ) {
            throw new IllegalStateException("Confidence must be between 0 and 1");
        }
    }

    public RouterResponse routingFallback(
        RouterRequest routerRequest,
        Long ticketId,
        Exception exception
    ) {
        log.error("Routing fallback triggered for ticket {}", ticketId, exception);

        llmOutputService.persistRoutingOutput(
            ticketId,
            "fallback",
            routerRequest.toString(),
            null,
            ParseStatus.TIMEOUT,
            exception.getMessage(),
            0
        );

        return RouterResponse.builder()
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
                                         exception.getMessage()
                                     )
                                 )
                             )
                             .build();
    }
}
