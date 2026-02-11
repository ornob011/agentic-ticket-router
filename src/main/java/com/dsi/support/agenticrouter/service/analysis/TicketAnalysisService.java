package com.dsi.support.agenticrouter.service.analysis;

import com.dsi.support.agenticrouter.dto.TicketAnalysisRequest;
import com.dsi.support.agenticrouter.dto.TicketAnalysisResult;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.LlmOutputService;
import com.dsi.support.agenticrouter.service.PromptService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketAnalysisService {

    private final OllamaChatModel ollamaChatModel;
    private final PromptService promptService;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;
    private final LlmOutputService llmOutputService;

    private ChatClient chatClient;

    public TicketAnalysisResult analyzeTicket(
        TicketAnalysisRequest ticketAnalysisRequest
    ) {
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

        if (Objects.isNull(chatClient)) {
            chatClient = ChatClient.builder(ollamaChatModel)
                                   .build();
        }

        String responseText = chatClient.prompt()
                                        .system(promptService.getSystemPrompt())
                                        .user(promptUserSpec -> promptUserSpec.text(promptService.getAnalysisPrompt())
                                                                              .param("content", ticketAnalysisRequest.getContent()))
                                        .call()
                                        .content();

        llmOutputService.persistAnalysisOutput(
            supportTicket,
            ticketAnalysisRequest.getContent(),
            responseText,
            LlmOutputType.ANALYSIS,
            ParseStatus.SUCCESS,
            null,
            0
        );

        auditService.recordTicketAnalysis(
            supportTicket.getId(),
            LlmOutputType.ANALYSIS.name(),
            true,
            null
        );

        TicketCategory category = parseCategory(
            responseText
        );

        log.info(
            "TicketAnalysis({}) SupportTicket(id:{}) Outcome(category:{},analysisLength:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            category,
            StringUtils.length(responseText)
        );

        return TicketAnalysisResult.builder()
                                   .analysis(responseText)
                                   .category(category)
                                   .build();
    }

    private TicketCategory parseCategory(
        String responseText
    ) {
        String[] lines = StringUtils.splitPreserveAllTokens(
            StringUtils.trimToEmpty(responseText),
            '\n'
        );

        String lastLine = StringUtils.trimToEmpty(
            lines[lines.length - 1]
        );

        String key = StringUtils.stripEnd(
            lastLine,
            StringUtils.CR
        );

        return EnumUtils.getEnumIgnoreCase(
            TicketCategory.class,
            key,
            TicketCategory.OTHER
        );
    }
}
