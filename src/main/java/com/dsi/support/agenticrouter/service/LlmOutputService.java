package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.LlmOutput;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.LlmOutputRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LlmOutputService {

    private final LlmOutputRepository llmOutputRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ObjectMapper objectMapper;

    public LlmOutput persistRoutingOutput(
        Long ticketId,
        String modelTag,
        String prompt,
        String response,
        ParseStatus parseStatus,
        String errorMessage,
        long latencyMs
    ) {
        return persistLlmOutput(
            ticketId,
            modelTag,
            prompt,
            response,
            parseStatus,
            errorMessage,
            latencyMs,
            LlmOutputType.ROUTING
        );
    }

    public LlmOutput persistAnalysisOutput(
        SupportTicket supportTicket,
        String requestContent,
        String responseContent,
        LlmOutputType outputType,
        ParseStatus parseStatus,
        String errorMessage,
        long latencyMs
    ) {
        JsonNode requestJson;
        try {
            requestJson = objectMapper.readTree(objectMapper.writeValueAsString(requestContent));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }

        JsonNode responseJson = null;

        try {
            if (Objects.nonNull(responseContent)) {
                responseJson = objectMapper.readTree(responseContent);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }

        LlmOutput.LlmOutputBuilder builder = LlmOutput.builder()
                                                      .ticket(supportTicket)
                                                      .modelTag("analyzer")
                                                      .outputType(outputType)
                                                      .rawRequest(requestJson)
                                                      .rawResponse(responseJson)
                                                      .parseStatus(parseStatus)
                                                      .errorMessage(errorMessage)
                                                      .latencyMs(latencyMs)
                                                      .repairAttempts(0);

        return llmOutputRepository.save(builder.build());
    }

    public LlmOutput getLatestRoutingOutput(Long ticketId) {
        return llmOutputRepository.findFirstByTicketIdAndOutputTypeOrderByCreatedAtDesc(
            ticketId,
            LlmOutputType.ROUTING
        ).orElse(null);
    }

    private LlmOutput persistLlmOutput(
        Long ticketId,
        String modelTag,
        String prompt,
        String response,
        ParseStatus parseStatus,
        String errorMessage,
        long latencyMs,
        LlmOutputType outputType
    ) {
        JsonNode requestJson;

        try {
            if (StringUtils.isNotBlank(prompt)) {
                requestJson = objectMapper.readTree(objectMapper.writeValueAsString(Map.of("prompt", prompt)));
            } else {
                requestJson = objectMapper.readTree(prompt);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }

        JsonNode responseJson = null;

        try {
            if (Objects.nonNull(response)) {
                responseJson = objectMapper.readTree(response);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
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

        LlmOutput.LlmOutputBuilder builder = LlmOutput.builder()
                                                      .ticket(supportTicket)
                                                      .modelTag(modelTag)
                                                      .outputType(outputType)
                                                      .rawRequest(requestJson)
                                                      .rawResponse(responseJson)
                                                      .parseStatus(parseStatus)
                                                      .errorMessage(errorMessage)
                                                      .latencyMs(latencyMs)
                                                      .repairAttempts(0);

        if (LlmOutputType.ROUTING.equals(outputType)) {
            builder.temperature(0.2);
        }

        return llmOutputRepository.save(builder.build());
    }
}
