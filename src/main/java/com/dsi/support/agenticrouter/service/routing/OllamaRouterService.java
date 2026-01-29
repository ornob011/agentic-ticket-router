package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.LlmOutput;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.dsi.support.agenticrouter.repository.LlmOutputRepository;
import com.dsi.support.agenticrouter.repository.ModelRegistryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

// TODO: Fix the class

/**
 * Service for calling Ollama LLM for ticket routing decisions.
 * Uses Spring AI for inference and validates responses against strict JSON
 * schema.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OllamaRouterService {

    private final OllamaChatModel ollamaChatModel;
    private final ModelRegistryRepository modelRegistryRepository;
    private final LlmOutputRepository llmOutputRepository;
    private final com.dsi.support.agenticrouter.repository.SupportTicketRepository supportTicketRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    private static final String ROUTING_PROMPT_TEMPLATE = """
        You are a support ticket routing assistant. Analyze the following ticket and provide a structured routing decision.
        
        TICKET INFORMATION:
        - Ticket Number: %s
        - Subject: %s
        - Customer: %s (Tier: %s)
        - Initial Message: %s
        
        CONVERSATION HISTORY:
        %s
        
        ROUTING INSTRUCTIONS:
        Analyze the ticket and respond with ONLY a valid JSON object (no additional text) with these exact fields:
        {
          "category": "BILLING" | "TECHNICAL" | "ACCOUNT" | "SHIPPING" | "SECURITY" | "OTHER",
          "priority": "CRITICAL" | "HIGH" | "MEDIUM" | "LOW",
          "queue": "BILLING_Q" | "TECH_Q" | "OPS_Q" | "SECURITY_Q" | "ACCOUNT_Q" | "GENERAL_Q",
          "next_action": "AUTO_REPLY" | "ASK_CLARIFYING" | "ASSIGN_QUEUE" | "ESCALATE" | "HUMAN_REVIEW",
          "confidence": 0.85,
          "clarifying_question": "Optional question if next_action is ASK_CLARIFYING",
          "draft_reply": "Optional reply if next_action is AUTO_REPLY",
          "rationale_tags": ["TAG1", "TAG2"]
        }
        
        IMPORTANT RULES:
        - category=SECURITY or tags containing THREAT/PII_RISK should have next_action=ESCALATE
        - If uncertain or confidence < 0.7, use next_action=HUMAN_REVIEW
        - confidence must be between 0.0 and 1.0
        - Only use the exact enum values listed above
        
        Respond with ONLY the JSON object, no markdown formatting, no explanations.
        """;

    /**
     * Get routing decision from Ollama with retry and circuit breaker.
     *
     * @param request  Routing context
     * @param ticketId Ticket ID for logging
     * @return Validated routing response
     */
    @Retry(name = "ollamaRouting", fallbackMethod = "routingFallback")
    @CircuitBreaker(name = "ollamaRouting", fallbackMethod = "routingFallback")
    public RouterResponse getRoutingDecision(RouterRequest request, Long ticketId) {
        log.debug("Calling Ollama for routing decision: ticketId={}", ticketId);

        Instant startTime = Instant.now();

        try {
            // Get active model configuration
            ModelRegistry activeModel = modelRegistryRepository.findByActiveTrue()
                                                               .stream()
                                                               .findFirst()
                                                               .orElseThrow(() -> new IllegalStateException("No active model configured"));

            // Build prompt
            String prompt = String.format(
                ROUTING_PROMPT_TEMPLATE,
                request.getTicketNo(),
                request.getSubject(),
                request.getCustomerName(),
                request.getCustomerTier(),
                request.getInitialMessage(),
                request.getConversationHistory());

            // Call Ollama via ChatClient
            // Note: Model configuration (temperature, format) should be set in
            // application.properties
            ChatClient chatClient = ChatClient.builder(ollamaChatModel).build();

            String responseText = chatClient.prompt()
                                            .user(prompt)
                                            .call()
                                            .content();

            long latencyMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();

            log.debug("Received response from Ollama in {}ms: {}", latencyMs, responseText);

            // Parse and validate response
            RouterResponse routerResponse = parseAndValidate(responseText);

            // Persist LLM output for audit
            persistLlmOutput(
                ticketId,
                activeModel.getModelTag(),
                prompt,
                responseText,
                ParseStatus.SUCCESS,
                null,
                latencyMs);

            return routerResponse;

        } catch (Exception e) {
            long latencyMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();

            log.error("Ollama routing failed for ticket: " + ticketId, e);

            // Persist failure
            persistLlmOutput(
                ticketId,
                "unknown",
                request.toString(),
                null,
                ParseStatus.MODEL_ERROR,
                e.getMessage(),
                latencyMs);

            throw new RuntimeException("Routing inference failed", e);
        }
    }

    /**
     * Parse JSON response and validate against schema and enum constraints.
     */
    private RouterResponse parseAndValidate(String jsonResponse) throws Exception {
        // Clean response (remove markdown code blocks if present)
        String cleanJson = jsonResponse.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        // Parse JSON
        RouterResponse response;
        try {
            response = objectMapper.readValue(cleanJson, RouterResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON response: {}", cleanJson, e);
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
        }

        // Validate using Bean Validation
        Set<ConstraintViolation<RouterResponse>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                                      .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                      .reduce((a, b) -> a + ", " + b)
                                      .orElse("Unknown validation error");

            log.error("Validation failed: {}", errors);
            throw new IllegalArgumentException("Validation failed: " + errors);
        }

        return response;
    }

    /**
     * Persist LLM output for audit and retraining.
     */
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
            JsonNode requestJson = objectMapper.readTree(objectMapper.writeValueAsString(Map.of("prompt", request)));
            JsonNode responseJson = response != null ? objectMapper.readTree(response) : null;

            // Fetch ticket for relationship
            com.dsi.support.agenticrouter.entity.SupportTicket ticket = ticketId != null
                ? supportTicketRepository.findById(ticketId).orElse(null)
                : null;

            LlmOutput output = LlmOutput.builder()
                                        .ticket(ticket)
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

        } catch (Exception e) {
            log.error("Failed to persist LLM output", e);
            // Don't throw - this is just for audit
        }
    }

    /**
     * Fallback method when Ollama is unavailable.
     */
    public RouterResponse routingFallback(RouterRequest request, Long ticketId, Throwable t) {
        log.warn("Using fallback routing for ticket: " + ticketId, t);

        // Return conservative routing decision
        return RouterResponse.builder()
                             .category(com.dsi.support.agenticrouter.enums.TicketCategory.OTHER)
                             .priority(com.dsi.support.agenticrouter.enums.TicketPriority.MEDIUM)
                             .queue(com.dsi.support.agenticrouter.enums.TicketQueue.GENERAL_Q)
                             .nextAction(com.dsi.support.agenticrouter.enums.NextAction.HUMAN_REVIEW)
                             .confidence(java.math.BigDecimal.valueOf(0.0))
                             .build();
    }
}
