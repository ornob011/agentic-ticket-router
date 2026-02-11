package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.enums.LlmOutputType;
import com.dsi.support.agenticrouter.enums.ParseStatus;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MessageCategoryService {

    private final OllamaChatModel ollamaChatModel;
    private final LlmOutputService llmOutputService;
    private final AuditService auditService;
    private final PromptService promptService;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;

    private ChatClient chatClient;

    public CategoryDetectionResult detectCategory(
        String content,
        Long supportTicketId
    ) {
        long startTime = System.currentTimeMillis();

        log.info(
            "CategoryDetection({}) SupportTicket(id:{}) Outcome(contentLength:{})",
            OperationalLogContext.PHASE_START,
            supportTicketId,
            StringUtils.length(content)
        );

        if (Objects.isNull(chatClient)) {
            chatClient = ChatClient.builder(ollamaChatModel)
                                   .build();
        }

        SupportTicket supportTicket = supportTicketRepository.findById(supportTicketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     supportTicketId
                                                                 )
                                                             );

        String categoryValues = Arrays.stream(TicketCategory.values())
                                      .map(Enum::name)
                                      .collect(Collectors.joining(" | "));

        List<TicketMessage> ticketMessages = ticketMessageRepository.findAllByTicket_IdOrderByCreatedAtAsc(
            supportTicketId
        );

        String conversationHistory = formatConversationHistory(
            ticketMessages
        );

        String responseText = chatClient.prompt()
                                        .system(promptService.getSystemPrompt())
                                        .user(userSpec -> userSpec
                                            .text(promptService.getCategoryDetectionPrompt())
                                            .param("content", content)
                                            .param("category", categoryValues)
                                            .param("current_category", supportTicket.getCurrentCategory().name())
                                            .param("conversation_history", conversationHistory))
                                        .call()
                                        .content();

        long latencyMs = System.currentTimeMillis() - startTime;

        String normalizedResponse = normalizeResponse(
            responseText
        );

        TicketCategory detectedCategory = parseCategory(
            normalizedResponse
        );

        log.info(
            "CategoryDetection({}) SupportTicket(id:{},status:{},currentCategory:{}) Outcome(detectedCategory:{},latencyMs:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            supportTicket.getCurrentCategory(),
            detectedCategory,
            latencyMs
        );

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

    private String formatConversationHistory(
        List<TicketMessage> ticketMessages
    ) {
        if (ticketMessages == null || ticketMessages.isEmpty()) {
            return "No previous messages.";
        }

        return ticketMessages.stream()
                             .map(this::formatMessage)
                             .collect(Collectors.joining("\n"));
    }

    private String formatMessage(
        TicketMessage message
    ) {
        String sender = switch (message.getMessageKind()) {
            case CUSTOMER_MESSAGE -> "Customer";
            case AGENT_MESSAGE -> "Agent";
            case SYSTEM_MESSAGE -> "System";
            case CLARIFYING_QUESTION -> "AI (Clarifying Question)";
            case AUTO_REPLY -> "AI (Automated Reply)";
            case INTERNAL_NOTE -> "Internal Note";
        };

        return String.format(
            "%s: %s",
            sender,
            message.getContent()
        );
    }

    private String normalizeResponse(
        String response
    ) {
        return StringUtils.strip(
            StringUtils.upperCase(StringUtils.trimToEmpty(response)),
            ".;,"
        );
    }

    private TicketCategory parseCategory(
        String response
    ) {
        return Arrays.stream(TicketCategory.values())
                     .sorted((category1, category2) -> Integer.compare(
                         category2.name().length(),
                         category1.name().length()
                     ))
                     .filter(ticketCategory -> response.contains(ticketCategory.name()))
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
