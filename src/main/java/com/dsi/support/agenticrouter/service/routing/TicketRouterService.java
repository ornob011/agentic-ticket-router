package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.enums.*;
import com.dsi.support.agenticrouter.service.ai.LlmOutputService;
import com.dsi.support.agenticrouter.service.ai.ModelService;
import com.dsi.support.agenticrouter.service.ai.PromptService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketRouterService {

    private final ModelService modelService;
    private final LlmOutputService llmOutputService;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;
    private final RoutingModelClient routingModelClient;
    private final RouterResponseContractValidator routerResponseContractValidator;

    @Retry(
        name = "llmRetry",
        fallbackMethod = "routingFallback"
    )
    @CircuitBreaker(
        name = "llmCircuit",
        fallbackMethod = "routingFallback"
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RouterResponse getRoutingDecision(
        RouterRequest routerRequest,
        Long ticketId
    ) {
        long startTime = System.currentTimeMillis();

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

        ModelRegistry activeModel = modelService.getActiveModel();

        JsonNode routingDecision = routingModelClient.requestRoutingDecision(
            routerRequest
        );

        RouterResponse routerResponse = mapBaseResponse(
            routingDecision,
            ticketId
        );

        routerResponseContractValidator.validate(
            routerResponse
        );

        long latencyMs = System.currentTimeMillis() - startTime;

        llmOutputService.persistRoutingOutput(
            ticketId,
            activeModel.getModelTag(),
            promptService.getRoutingPrompt().toString(),
            routerResponse,
            ParseStatus.SUCCESS,
            null,
            latencyMs
        );

        log.info(
            "RoutingDecision({}) SupportTicket(id:{}) Model(tag:{}) RouterResponse(category:{},priority:{},queue:{},nextAction:{},confidence:{},latencyMs:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            activeModel.getModelTag(),
            routerResponse.getCategory(),
            routerResponse.getPriority(),
            routerResponse.getQueue(),
            routerResponse.getNextAction(),
            routerResponse.getConfidence(),
            latencyMs
        );

        return routerResponse;
    }

    private RouterResponse mapBaseResponse(
        JsonNode baseRoutingJson,
        Long ticketId
    ) {
        try {
            return objectMapper.treeToValue(
                baseRoutingJson,
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

            throw new IllegalStateException("Failed to map router response JSON", exception);
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
            classifyParseStatus(throwable),
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

    private ParseStatus classifyParseStatus(
        Throwable throwable
    ) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof TimeoutException) {
                return ParseStatus.TIMEOUT;
            }
            cause = cause.getCause();
        }

        return ParseStatus.MODEL_ERROR;
    }
}
