package com.dsi.support.agenticrouter.service.analysis;

import com.dsi.support.agenticrouter.dto.TicketAnalysisRequest;
import com.dsi.support.agenticrouter.dto.TicketAnalysisResult;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.dsi.support.agenticrouter.enums.TicketAnalysis;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.prompts.TicketAnalysisPrompts;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.AuditService;
import com.dsi.support.agenticrouter.service.LlmOutputService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketAnalysisService {

    private final OllamaChatModel ollamaChatModel;
    private final TicketAnalysisPrompts ticketAnalysisPrompts;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditService auditService;
    private final LlmOutputService llmOutputService;

    private ChatClient chatClient;

    public TicketAnalysisResult analyzeTicketSection(
        TicketAnalysisRequest request,
        TicketAnalysis ticketAnalysis
    ) {
        SupportTicket supportTicket = supportTicketRepository.findById(request.getTicketId())
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     request.getTicketId()
                                                                 )
                                                             );

        if (Objects.isNull(chatClient)) {
            chatClient = ChatClient.builder(ollamaChatModel)
                                   .defaultSystem(ticketAnalysisPrompts.getAnalyzerSystemMsg())
                                   .build();
        }

        Resource userMsgResource;
        userMsgResource = ticketAnalysisPrompts.getAnalysisTemplate(ticketAnalysis);

        Resource markdownResource;
        markdownResource = ticketAnalysisPrompts.getMarkdownTemplate(ticketAnalysis);

        String responseText;
        try {
            responseText = chatClient.prompt()
                                     .user(promptUserSpec -> promptUserSpec.text(userMsgResource)
                                                                           .param("document", request.getContent())
                                                                           .param("markdownTemplate", markdownResource))
                                     .call()
                                     .content();

            llmOutputService.persistAnalysisOutput(
                supportTicket,
                request.getContent(),
                responseText,
                mapTicketSectionToOutputType(ticketAnalysis),
                ParseStatus.SUCCESS,
                null,
                0
            );

            auditService.recordTicketAnalysis(supportTicket.getId(), ticketAnalysis.name(), true, null);

            return TicketAnalysisResult.builder()
                                       .section(ticketAnalysis)
                                       .extractedMarkdown(responseText)
                                       .build();

        } catch (Exception exception) {
            log.error("Failed to analyze ticket section {} for ticket {}", ticketAnalysis, request.getTicketId(), exception);

            llmOutputService.persistAnalysisOutput(
                supportTicket,
                request.getContent(),
                null,
                mapTicketSectionToOutputType(ticketAnalysis),
                ParseStatus.MODEL_ERROR,
                exception.getMessage(),
                0
            );

            auditService.recordTicketAnalysis(supportTicket.getId(), ticketAnalysis.name(), false, exception.getMessage());

            return TicketAnalysisResult.builder()
                                       .section(ticketAnalysis)
                                       .extractedMarkdown(null)
                                       .confidence(0.0)
                                       .build();
        }
    }

    private LlmOutputType mapTicketSectionToOutputType(TicketAnalysis ticketAnalysis) {
        return switch (ticketAnalysis) {
            case TICKET_DETAILS -> LlmOutputType.ANALYSIS_TICKET_DETAILS;
            case CUSTOMER_INFORMATION -> LlmOutputType.ANALYSIS_CUSTOMER_INFO;
            case CONVERSATION_HISTORY -> LlmOutputType.ANALYSIS_CONVERSATION_HISTORY;
            case TECHNICAL_DETAILS -> LlmOutputType.ANALYSIS_TECHNICAL_DETAILS;
            case ACTIONS_REQUIRED -> LlmOutputType.ANALYSIS_ACTIONS_REQUIRED;
        };
    }
}
