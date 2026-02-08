package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MessageCategoryService {

    private static final double DEFAULT_TEMPERATURE = 0.0;
    private static final int NUM_CONTEXT = 32768;

    private final OllamaChatModel ollamaChatModel;
    private final LlmOutputService llmOutputService;
    private final AuditService auditService;

    @Value("classpath:/prompts/system.st")
    private Resource systemPromptResource;

    @Value("classpath:/prompts/category_detection.st")
    private Resource categoryDetectionPromptResource;

    private ChatClient chatClient;

    public CategoryDetectionResult detectCategory(
        String content,
        SupportTicket supportTicket
    ) {
        long startTime = System.currentTimeMillis();

        if (Objects.isNull(chatClient)) {
            chatClient = ChatClient.builder(ollamaChatModel)
                                   .defaultOptions(
                                       OllamaChatOptions.builder()
                                                        .numCtx(NUM_CONTEXT)
                                                        .temperature(DEFAULT_TEMPERATURE)
                                                        .build()
                                   )
                                   .build();
        }

        try {
            String responseText = chatClient.prompt()
                                            .system(systemPromptResource)
                                            .user(userSpec -> userSpec
                                                .text(categoryDetectionPromptResource)
                                                .param("content", content))
                                            .call()
                                            .content();

            long latencyMs = System.currentTimeMillis() - startTime;

            String normalizedResponse = normalizeResponse(responseText);
            TicketCategory detectedCategory = parseCategory(normalizedResponse);

            llmOutputService.persistAnalysisOutput(
                supportTicket,
                content,
                "Detected category: " + detectedCategory.name(),
                LlmOutputType.CATEGORY_DETECTION,
                ParseStatus.SUCCESS,
                null,
                latencyMs
            );

            auditService.recordEvent(
                AuditEventType.TICKET_ANALYSIS_EXECUTED,
                supportTicket.getId(),
                null,
                "Category detection: " + detectedCategory.name(),
                null
            );

            return CategoryDetectionResult.builder()
                                          .detectedCategory(detectedCategory)
                                          .success(true)
                                          .build();

        } catch (Exception exception) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Failed to detect category for ticket {}", supportTicket.getId(), exception);

            llmOutputService.persistAnalysisOutput(
                supportTicket,
                content,
                null,
                LlmOutputType.CATEGORY_DETECTION,
                ParseStatus.MODEL_ERROR,
                exception.getMessage(),
                latencyMs
            );

            auditService.recordEvent(
                AuditEventType.TICKET_ANALYSIS_FAILED,
                supportTicket.getId(),
                null,
                "Category detection failed: " + exception.getMessage(),
                null
            );

            return CategoryDetectionResult.builder()
                                          .success(false)
                                          .errorMessage(exception.getMessage())
                                          .build();
        }
    }

    public boolean isSameCategory(
        TicketCategory currentCategory,
        TicketCategory detectedCategory
    ) {
        if (Objects.isNull(currentCategory)) {
            return true;
        }

        return Objects.equals(currentCategory, detectedCategory);
    }

    private String normalizeResponse(
        String response
    ) {
        if (response == null) {
            return StringUtils.EMPTY;
        }

        return StringUtils.normalizeSpace(response)
                          .toUpperCase()
                          .replace(' ', '_');
    }

    private TicketCategory parseCategory(
        String response
    ) {
        return Arrays.stream(TicketCategory.values())
                     .filter(ticketCategory -> ticketCategory.name().equals(response))
                     .findFirst()
                     .orElse(TicketCategory.OTHER);
    }

    @Data
    @Builder
    public static class CategoryDetectionResult {
        private TicketCategory detectedCategory;
        private boolean success;
        private String errorMessage;
    }
}
