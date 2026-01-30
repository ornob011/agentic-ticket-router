package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.LlmOutput;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.LlmOutputRepository;
import com.dsi.support.agenticrouter.repository.ModelRegistryRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.resource.routing.TicketRoutingPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
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
    private final SupportTicketRepository supportTicketRepository;
    private final ObjectMapper objectMapper;
    private final TicketRoutingPrompt ticketRoutingPrompt;

    public RouterResponse getRoutingDecision(
        RouterRequest request,
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

        Resource routingPromptResource = ticketRoutingPrompt.getRoutingPrompt();

        ChatClient chatClient = ChatClient.builder(ollamaChatModel)
                                          .build();

        ChatResponse response = chatClient.prompt()
                                          .user(promptUserSpec -> promptUserSpec.text(routingPromptResource)
                                                                                .param("category", categoryValues)
                                                                                .param("priority", priorityValues)
                                                                                .param("queue", queueValues)
                                                                                .param("next_action", nextActionValues)
                                                                                .param("ticketNo", request.getTicketNo())
                                                                                .param("subject", request.getSubject())
                                                                                .param("customerName", request.getCustomerName())
                                                                                .param("customerTier", request.getCustomerTier())
                                                                                .param("initialMessage", request.getInitialMessage())
                                                                                .param("conversationHistory", request.getConversationHistory()))
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
            routerResponse = objectMapper.readValue(responseText, RouterResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }

        persistLlmOutput(
            ticketId,
            activeModel.getModelTag(),
            routingPromptResource.toString(),
            responseText,
            ParseStatus.SUCCESS,
            null,
            0
        );

        return routerResponse;
    }

    private void persistLlmOutput(
        Long ticketId,
        String modelTag,
        String request,
        String response,
        ParseStatus parseStatus,
        String errorMessage,
        long latencyMs
    ) {
        try {
            JsonNode requestJson = objectMapper.readTree(
                objectMapper.writeValueAsString(
                    Map.of("prompt", request)
                )
            );

            JsonNode responseJson = null;
            if (Objects.nonNull(response)) {
                responseJson = objectMapper.readTree(response);
            }

            SupportTicket supportTicket = null;
            if (Objects.nonNull(ticketId)) {
                supportTicket = supportTicketRepository.findById(ticketId)
                                                       .orElseThrow(
                                                           DataNotFoundException.supplier(
                                                               SupportTicket.class,
                                                               ticketId
                                                           )
                                                       );
            }

            LlmOutput output = LlmOutput.builder()
                                        .ticket(supportTicket)
                                        .modelTag(modelTag)
                                        .rawRequest(requestJson)
                                        .rawResponse(responseJson)
                                        .parseStatus(parseStatus)
                                        .errorMessage(errorMessage)
                                        .latencyMs(latencyMs)
                                        .repairAttempts(0)
                                        .temperature(0.2)
                                        .build();

            llmOutputRepository.save(output);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
