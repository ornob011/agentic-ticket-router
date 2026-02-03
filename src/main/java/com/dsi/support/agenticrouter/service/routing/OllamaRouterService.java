package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.LlmOutputRepository;
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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
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
    private final LlmOutputRepository llmOutputRepository;
    private final LlmOutputService llmOutputService;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    private static final int MAX_REPAIR_ATTEMPTS = 2;

    @Retry(name = "ollamaRetry", fallbackMethod = "routingFallback")
    @CircuitBreaker(name = "ollamaCircuit", fallbackMethod = "routingFallback")
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
                                          .build();

        ChatResponse response = chatClient.prompt()
                                          .system(promptService.getSystemPrompt())
                                          .user(promptUserSpec -> promptUserSpec.text(promptService.getRoutingPrompt())
                                                                                .param("category", categoryValues)
                                                                                .param("priority", priorityValues)
                                                                                .param("queue", queueValues)
                                                                                .param("next_action", nextActionValues)
                                                                                .param("ticketNo", routerRequest.getTicketNo())
                                                                                .param("subject", routerRequest.getSubject())
                                                                                .param("customerName", routerRequest.getCustomerName())
                                                                                .param("customerTier", routerRequest.getCustomerTier())
                                                                                .param("initialMessage", routerRequest.getInitialMessage())
                                                                                .param("conversationHistory", routerRequest.getConversationHistory())
                                                                                .param("analysis", routerRequest.getAnalysis()))
                                          .call()
                                          .chatResponse();

        if (Objects.isNull(response) || response.getResults().isEmpty()) {
            throw new IllegalStateException("LLM returned empty response");
        }

        String responseText = response.getResults()
                                      .getFirst()
                                      .getOutput().getText();

        RouterResponse routerResponse;
        try {
            routerResponse = objectMapper.readValue(
                responseText,
                RouterResponse.class
            );

            validateRouterResponse(
                routerResponse
            );
        } catch (JsonProcessingException | IllegalStateException exception) {
            log.error("Failed to parse LLM response", exception);

            var lastOutput = llmOutputRepository.findFirstByTicketIdAndOutputTypeOrderByCreatedAtDesc(
                ticketId,
                LlmOutputType.ROUTING
            ).orElse(null);

            if (Objects.nonNull(lastOutput) && lastOutput.getRepairAttempts() < MAX_REPAIR_ATTEMPTS) {
                return attemptRepair(
                    routerRequest,
                    ticketId,
                    responseText,
                    exception,
                    lastOutput.getRepairAttempts() + 1
                );
            }

            llmOutputService.persistRoutingOutput(
                ticketId,
                activeModel.getModelTag(),
                promptService.getRoutingPrompt().toString(),
                responseText,
                ParseStatus.INVALID_JSON,
                exception.getMessage(),
                0
            );

            return getFallbackResponse(
                "Invalid JSON from LLM after repair attempts"
            );
        }

        llmOutputService.persistRoutingOutput(
            ticketId,
            activeModel.getModelTag(),
            promptService.getRoutingPrompt().toString(),
            responseText,
            ParseStatus.SUCCESS,
            null,
            0
        );

        return routerResponse;
    }

    private RouterResponse attemptRepair(
        RouterRequest request,
        Long ticketId,
        String failedResponse,
        Exception originalException,
        int attempt
    ) {
        log.info("Attempting repair ({}/{}) for ticket {}", attempt, MAX_REPAIR_ATTEMPTS, ticketId);

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
                                          .build();

        ChatResponse response = chatClient.prompt()
                                          .system(promptService.getSystemPrompt())
                                          .user(promptUserSpec -> promptUserSpec.text(promptService.getRepairPrompt())
                                                                                .param("failedResponse", failedResponse)
                                                                                .param("errorMessage", originalException.getMessage())
                                                                                .param("categoryValues", categoryValues)
                                                                                .param("priorityValues", priorityValues)
                                                                                .param("queueValues", queueValues)
                                                                                .param("nextActionValues", nextActionValues)
                                                                                .param("ticketNo", request.getTicketNo())
                                                                                .param("subject", request.getSubject())
                                                                                .param("customerName", request.getCustomerName())
                                                                                .param("initialMessage", request.getInitialMessage()))
                                          .call()
                                          .chatResponse();

        if (Objects.isNull(response) || response.getResults().isEmpty()) {
            llmOutputService.persistRoutingOutput(
                ticketId,
                activeModel.getModelTag(),
                "repair-attempt",
                null,
                ParseStatus.REPAIR_FAILED,
                "Empty response from repair attempt",
                attempt
            );

            return getFallbackResponse("Repair failed: Empty response");
        }

        String responseText = response.getResults().getFirst().getOutput().getText();

        try {
            RouterResponse routerResponse = objectMapper.readValue(
                responseText,
                RouterResponse.class
            );

            validateRouterResponse(
                routerResponse
            );

            llmOutputService.persistRoutingOutput(
                ticketId,
                activeModel.getModelTag(),
                "repair-attempt",
                responseText,
                ParseStatus.REPAIR_SUCCESS,
                null,
                attempt
            );

            return routerResponse;
        } catch (Exception exception) {
            log.error("Repair attempt {} failed", attempt, exception);

            llmOutputService.persistRoutingOutput(
                ticketId,
                activeModel.getModelTag(),
                "repair-attempt",
                responseText,
                ParseStatus.REPAIR_FAILED,
                exception.getMessage(),
                attempt
            );

            if (attempt < MAX_REPAIR_ATTEMPTS) {
                return attemptRepair(
                    request,
                    ticketId,
                    responseText,
                    exception,
                    attempt + 1
                );
            }

            return getFallbackResponse(
                String.format("Repair failed after %d attempts", MAX_REPAIR_ATTEMPTS)
            );
        }
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

        if (routerResponse.getConfidence().compareTo(BigDecimal.ZERO) < 0 || routerResponse.getConfidence().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("confidence must be between 0 and 1");
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

        return getFallbackResponse(
            String.format("Model inference failed: %s", exception.getMessage())
        );
    }

    private RouterResponse getFallbackResponse(
        String errorMessage
    ) {
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
                                     String.format("MODEL_ERROR:%s", errorMessage)
                                 )
                             )
                             .build();
    }
}
