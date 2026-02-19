package com.dsi.support.agenticrouter.service.analysis;

import com.dsi.support.agenticrouter.dto.TicketAnalysisRequest;
import com.dsi.support.agenticrouter.dto.TicketAnalysisResult;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.ai.LlmOutputService;
import com.dsi.support.agenticrouter.service.ai.LlmPromptCaller;
import com.dsi.support.agenticrouter.service.ai.LlmResponseTextExtractor;
import com.dsi.support.agenticrouter.service.ai.PromptService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketAnalysisService {

    private final ChatModel chatModel;
    private final PromptService promptService;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;
    private final LlmOutputService llmOutputService;
    private final AnalysisCategoryParser analysisCategoryParser;
    private final LlmPromptCaller llmPromptCaller;
    private final LlmResponseTextExtractor llmResponseTextExtractor;

    public TicketAnalysisResult analyzeTicket(
        TicketAnalysisRequest ticketAnalysisRequest
    ) {
        long startTime = System.currentTimeMillis();

        log.info(
            "TicketAnalysis({}) SupportTicket(id:{}) Outcome(contentLength:{})",
            OperationalLogContext.PHASE_START,
            ticketAnalysisRequest.getTicketId(),
            StringUtils.length(ticketAnalysisRequest.getContent())
        );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketAnalysisRequest.getTicketId())
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketAnalysisRequest.getTicketId()
                                                                 )
                                                             );

        String categoryTokens = Arrays.stream(TicketCategory.values())
                                      .map(Enum::name)
                                      .collect(Collectors.joining(" | "));

        String categoryContract = String.format(
            "The final line must be exactly one category token from this list: %s",
            categoryTokens
        );

        String responseText = llmResponseTextExtractor.extractRequiredContent(
            llmPromptCaller.call(
                chatModel,
                promptUserSpec -> promptUserSpec.text(promptService.getAnalysisPrompt())
                                                .param("content", ticketAnalysisRequest.getContent())
                                                .param("category_contract", categoryContract)
                                                .param("fallback_category", TicketCategory.OTHER.name())
            ),
            "ticket_analysis"
        );

        long latencyMs = System.currentTimeMillis() - startTime;

        llmOutputService.persistAnalysisOutput(
            supportTicket,
            ticketAnalysisRequest.getContent(),
            responseText,
            LlmOutputType.ANALYSIS,
            ParseStatus.SUCCESS,
            null,
            latencyMs
        );

        auditService.recordTicketAnalysis(
            supportTicket.getId(),
            LlmOutputType.ANALYSIS.name(),
            true,
            null
        );

        TicketCategory category = analysisCategoryParser.parseFromModelResponse(
            responseText
        );

        log.info(
            "TicketAnalysis({}) SupportTicket(id:{}) Outcome(category:{},analysisLength:{},latencyMs:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            category,
            StringUtils.length(responseText),
            latencyMs
        );

        return TicketAnalysisResult.builder()
                                   .analysis(responseText)
                                   .category(category)
                                   .build();
    }

}
