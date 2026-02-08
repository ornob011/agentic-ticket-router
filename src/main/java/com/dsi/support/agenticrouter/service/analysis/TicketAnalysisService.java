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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
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
        SupportTicket supportTicket = supportTicketRepository.findById(ticketAnalysisRequest.getTicketId())
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketAnalysisRequest.getTicketId()
                                                                 )
                                                             );

        if (Objects.isNull(chatClient)) {
            chatClient = ChatClient.builder(ollamaChatModel)
                                   .defaultOptions(
                                       OllamaChatOptions.builder()
                                                        .numCtx(32768)
                                                        .temperature(0.0)
                                                        .build()
                                   )
                                   .build();
        }

        String responseText;
        try {
            responseText = chatClient.prompt()
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

            return TicketAnalysisResult.builder()
                                       .analysis(responseText)
                                       .category(category)
                                       .build();

        } catch (Exception exception) {
            log.error("Failed to analyze ticket {}", ticketAnalysisRequest.getTicketId(), exception);

            llmOutputService.persistAnalysisOutput(
                supportTicket,
                ticketAnalysisRequest.getContent(),
                null,
                LlmOutputType.ANALYSIS,
                ParseStatus.MODEL_ERROR,
                exception.getMessage(),
                0
            );

            auditService.recordTicketAnalysis(
                supportTicket.getId(),
                LlmOutputType.ANALYSIS.name(),
                false,
                exception.getMessage()
            );

            return TicketAnalysisResult.builder()
                                       .analysis(null)
                                       .category(null)
                                       .confidence(0.0)
                                       .build();
        }
    }

    private TicketCategory parseCategory(String responseText) {
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
