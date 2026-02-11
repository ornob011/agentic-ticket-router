package com.dsi.support.agenticrouter.service.ai;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.LlmOutput;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.LlmOutputRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmOutputService {

    private static final String ANALYZER_MODEL_TAG = "analyzer";
    private static final double DEFAULT_TEMPERATURE = 0.2;

    private final LlmOutputRepository llmOutputRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRoutingOutput(
        Long ticketId,
        String modelTag,
        String promptText,
        RouterResponse routingResponse,
        ParseStatus parseStatus,
        String errorMessage,
        long latencyMs
    ) {
        persistRoutingOutputInternal(
            ticketId,
            modelTag,
            promptText,
            routingResponse,
            parseStatus,
            errorMessage,
            latencyMs
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRoutingOutputInNewTransaction(
        Long ticketId,
        String modelTag,
        String promptText,
        RouterResponse routingResponse,
        ParseStatus parseStatus,
        String errorMessage,
        long latencyMs
    ) {
        persistRoutingOutputInternal(
            ticketId,
            modelTag,
            promptText,
            routingResponse,
            parseStatus,
            errorMessage,
            latencyMs
        );
    }

    private void persistRoutingOutputInternal(
        Long ticketId,
        String modelTag,
        String promptText,
        RouterResponse routingResponse,
        ParseStatus parseStatus,
        String errorMessage,
        long latencyMs
    ) {
        log.debug(
            "LlmOutputPersist({}) SupportTicket(id:{}) LlmOutput(type:{},modelTag:{},parseStatus:{},latencyMs:{})",
            OperationalLogContext.PHASE_START,
            ticketId,
            LlmOutputType.ROUTING,
            modelTag,
            parseStatus,
            latencyMs
        );

        Objects.requireNonNull(ticketId, "ticketId is required");
        Objects.requireNonNull(modelTag, "modelTag is required");
        Objects.requireNonNull(parseStatus, "parseStatus is required");

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        ObjectNode rawRequestJson = objectMapper.createObjectNode();
        String normalizedPromptText = StringUtils.trimToNull(promptText);

        if (StringUtils.isNotBlank(normalizedPromptText)) {
            rawRequestJson.put("prompt", normalizedPromptText);
        }

        JsonNode rawResponseJson = null;
        if (Objects.nonNull(routingResponse)) {
            rawResponseJson = objectMapper.valueToTree(routingResponse);
        }

        LlmOutput routingOutput = LlmOutput.builder()
                                           .ticket(supportTicket)
                                           .modelTag(modelTag)
                                           .outputType(LlmOutputType.ROUTING)
                                           .rawRequest(rawRequestJson)
                                           .rawResponse(rawResponseJson)
                                           .parseStatus(parseStatus)
                                           .errorMessage(errorMessage)
                                           .latencyMs(latencyMs)
                                           .repairAttempts(0)
                                           .temperature(DEFAULT_TEMPERATURE)
                                           .build();

        llmOutputRepository.save(routingOutput);

        log.info(
            "LlmOutputPersist({}) SupportTicket(id:{}) LlmOutput(id:{},type:{},modelTag:{},parseStatus:{},latencyMs:{})",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId,
            routingOutput.getId(),
            routingOutput.getOutputType(),
            routingOutput.getModelTag(),
            routingOutput.getParseStatus(),
            routingOutput.getLatencyMs()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistAnalysisOutput(
        SupportTicket supportTicket,
        String requestContent,
        String analysisText,
        LlmOutputType outputType,
        ParseStatus parseStatus,
        String errorMessage,
        long latencyMs
    ) {
        log.debug(
            "LlmOutputPersist({}) SupportTicket(id:{}) LlmOutput(type:{},modelTag:{},parseStatus:{},latencyMs:{})",
            OperationalLogContext.PHASE_START,
            Objects.nonNull(supportTicket) ? supportTicket.getId() : null,
            outputType,
            ANALYZER_MODEL_TAG,
            parseStatus,
            latencyMs
        );

        Objects.requireNonNull(supportTicket, "supportTicket is required");
        Objects.requireNonNull(requestContent, "requestContent is required");
        Objects.requireNonNull(outputType, "outputType is required");
        Objects.requireNonNull(parseStatus, "parseStatus is required");

        ObjectNode rawRequestJson = objectMapper.createObjectNode();
        rawRequestJson.put("content", requestContent);

        JsonNode rawResponseJson = null;
        String normalizedAnalysisText = StringUtils.trimToNull(analysisText);

        if (StringUtils.isNotBlank(normalizedAnalysisText)) {
            ObjectNode analysisJson = objectMapper.createObjectNode();

            analysisJson.put("analysis", normalizedAnalysisText);

            rawResponseJson = analysisJson;
        }

        LlmOutput analysisOutput = LlmOutput.builder()
                                            .ticket(supportTicket)
                                            .modelTag(ANALYZER_MODEL_TAG)
                                            .outputType(outputType)
                                            .rawRequest(rawRequestJson)
                                            .rawResponse(rawResponseJson)
                                            .parseStatus(parseStatus)
                                            .errorMessage(errorMessage)
                                            .latencyMs(latencyMs)
                                            .repairAttempts(0)
                                            .build();

        llmOutputRepository.save(analysisOutput);

        log.info(
            "LlmOutputPersist({}) SupportTicket(id:{}) LlmOutput(id:{},type:{},modelTag:{},parseStatus:{},latencyMs:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            analysisOutput.getId(),
            analysisOutput.getOutputType(),
            analysisOutput.getModelTag(),
            analysisOutput.getParseStatus(),
            analysisOutput.getLatencyMs()
        );
    }
}
