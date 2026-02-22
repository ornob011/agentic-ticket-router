package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.*;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.VectorStoreMetadataKey;
import com.dsi.support.agenticrouter.model.TicketAutonomousMetadata;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.knowledge.KnowledgeBaseVectorStore;
import com.dsi.support.agenticrouter.service.learning.RoutingPatternMatcherService;
import com.dsi.support.agenticrouter.service.memory.CustomerContextEnrichmentService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingRequestFactory {

    private static final int TOP_K_ARTICLES = 5;
    private static final double ARTICLE_SIMILARITY_THRESHOLD = 0.82;
    private static final int MIN_QUERY_LENGTH = 10;

    private static final int MAX_AUTONOMOUS_ACTIONS = 5;
    private static final int MAX_QUESTIONS = 3;
    private static final int MAX_CONVERSATION_MESSAGES = 5;

    private static final String TITLE_SEPARATOR = System.lineSeparator() + System.lineSeparator();
    private static final String UNKNOWN_TITLE = "Unknown Title";
    private static final String NO_PREVIOUS_QUESTION = "None";

    private final TicketMessageRepository ticketMessageRepository;
    private final KnowledgeBaseVectorStore knowledgeBaseVectorStore;
    private final CustomerContextEnrichmentService customerContextEnrichmentService;
    private final RoutingPatternMatcherService routingPatternMatcherService;

    public RouterRequest buildRouterRequest(
        SupportTicket supportTicket
    ) {
        List<TicketMessage> messages = ticketMessageRepository.findByTicketIdWithAuthorOrderByCreatedAtAsc(
            supportTicket.getId()
        );

        String initialMessage = getInitialMessage(messages);

        String conversationHistory = buildConversationText(
            supportTicket,
            messages
        );

        String latestCustomerMessage = getLatestMessage(
            messages,
            this::isCustomerMessage,
            initialMessage
        );

        String latestAssistantMessage = getLatestMessage(
            messages,
            this::isAssistantMessage,
            StringUtils.EMPTY
        );

        String customerTierCode = supportTicket.getCustomer()
                                               .getCustomerProfile()
                                               .getCustomerTier()
                                               .getCode();

        TicketAutonomousMetadata autonomousMetadata = supportTicket.getAutonomousMetadata();

        String lastClarifyingQuestion = Optional.ofNullable(autonomousMetadata)
                                                .map(TicketAutonomousMetadata::getLastClarifyingQuestion)
                                                .filter(StringUtils::isNotBlank)
                                                .orElse(NO_PREVIOUS_QUESTION);

        CompletableFuture<List<ArticleSearchResult>> articlesFuture = CompletableFuture.supplyAsync(
            () -> searchRelevantArticles(
                supportTicket.getSubject(),
                initialMessage
            )
        );

        CompletableFuture<List<PatternHint>> patternsFuture = CompletableFuture.supplyAsync(
            () -> routingPatternMatcherService.findRelevantPatterns(
                Objects.requireNonNullElse(
                    supportTicket.getCurrentCategory(),
                    TicketCategory.OTHER
                ),
                supportTicket.getSubject()
            )
        );

        CompletableFuture.allOf(
            articlesFuture,
            patternsFuture
        ).join();

        List<ArticleSearchResult> relevantArticles = articlesFuture.join();
        List<PatternHint> relevantPatterns = patternsFuture.join();

        int remainingActions = MAX_AUTONOMOUS_ACTIONS - Optional.ofNullable(autonomousMetadata)
                                                                .map(TicketAutonomousMetadata::getAutonomousActionCount)
                                                                .orElse(0);

        int questionsAsked = Optional.ofNullable(autonomousMetadata)
                                     .map(TicketAutonomousMetadata::getQuestionCount)
                                     .orElse(0);

        return RouterRequest.builder()
                            .ticketId(supportTicket.getId())
                            .ticketNo(supportTicket.getFormattedTicketNo())
                            .subject(supportTicket.getSubject())
                            .customerName(supportTicket.getCustomer().getFullName())
                            .customerTier(customerTierCode)
                            .initialMessage(initialMessage)
                            .conversationHistory(conversationHistory)
                            .latestCustomerMessage(latestCustomerMessage)
                            .latestAssistantMessage(latestAssistantMessage)
                            .previousClarifyingQuestion(lastClarifyingQuestion)
                            .relevantArticles(relevantArticles)
                            .relevantPatterns(relevantPatterns)
                            .remainingActions(remainingActions)
                            .questionsAsked(questionsAsked)
                            .maxQuestions(MAX_QUESTIONS)
                            .maxActions(MAX_AUTONOMOUS_ACTIONS)
                            .build();
    }

    private List<ArticleSearchResult> searchRelevantArticles(
        String subject,
        String initialMessage
    ) {
        String vectorQuery = buildVectorQuery(
            subject,
            initialMessage
        );

        if (vectorQuery.length() < MIN_QUERY_LENGTH) {
            log.debug(
                "RelevantArticleSearch({}) Outcome(queryLength:{},minLength:{},reason:{})",
                OperationalLogContext.PHASE_SKIP,
                vectorQuery.length(),
                MIN_QUERY_LENGTH,
                "query_too_short"
            );

            return new ArrayList<>();
        }

        List<Document> documents = knowledgeBaseVectorStore.searchSimilar(
            vectorQuery,
            TOP_K_ARTICLES,
            ARTICLE_SIMILARITY_THRESHOLD
        );

        log.debug(
            "RelevantArticleSearch({}) Outcome(queryLength:{},topK:{},similarityThreshold:{},resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            vectorQuery.length(),
            TOP_K_ARTICLES,
            ARTICLE_SIMILARITY_THRESHOLD,
            documents.size()
        );

        return documents.stream()
                        .map(this::toArticleSearchResult)
                        .toList();
    }

    private String buildVectorQuery(
        String subject,
        String initialMessage
    ) {
        return StringUtils.trim(
            StringUtils.joinWith(
                StringUtils.SPACE,
                StringUtils.defaultString(subject),
                StringUtils.defaultString(initialMessage)
            )
        );
    }

    private ArticleSearchResult toArticleSearchResult(
        Document document
    ) {
        ArticleSearchResult.ArticleSearchResultBuilder builder = ArticleSearchResult.builder();

        metadataString(document, VectorStoreMetadataKey.ARTICLE_ID)
            .flatMap(this::parseLong)
            .ifPresent(builder::articleId);

        builder.title(
            extractArticleTitle(document.getText())
        );

        builder.similarityScore(
            0.85
        );

        metadataString(document, VectorStoreMetadataKey.CATEGORY)
            .ifPresent(builder::category);

        metadataString(document, VectorStoreMetadataKey.PRIORITY)
            .flatMap(this::parseInt)
            .ifPresentOrElse(
                builder::priority,
                () -> builder.priority(0)
            );

        metadataString(document, VectorStoreMetadataKey.ARTICLE_TYPE)
            .ifPresent(builder::articleType);

        return builder.build();
    }

    private Optional<String> metadataString(
        Document document,
        VectorStoreMetadataKey metadataKey
    ) {
        return Optional.ofNullable(document.getMetadata().get(metadataKey.name()))
                       .map(Object::toString)
                       .map(StringNormalizationUtils::trimToNull);
    }

    private Optional<Long> parseLong(
        String numericText
    ) {
        return Optional.ofNullable(StringNormalizationUtils.trimToNull(numericText))
                       .filter(StringUtils::isNumeric)
                       .map(Long::valueOf);
    }

    private Optional<Integer> parseInt(
        String numericText
    ) {
        return Optional.ofNullable(StringNormalizationUtils.trimToNull(numericText))
                       .filter(StringUtils::isNumeric)
                       .map(Integer::valueOf);
    }

    private String extractArticleTitle(
        String documentText
    ) {
        String safeDocumentText = StringUtils.defaultString(documentText);

        String[] titleAndBody = StringUtils.splitByWholeSeparatorPreserveAllTokens(
            safeDocumentText,
            TITLE_SEPARATOR,
            2
        );

        return Optional.ofNullable(titleAndBody)
                       .filter(parts -> parts.length > 0)
                       .map(parts -> StringNormalizationUtils.trimToNull(parts[0]))
                       .orElse(UNKNOWN_TITLE);
    }

    private String getInitialMessage(
        List<TicketMessage> ticketMessages
    ) {
        return ticketMessages.stream()
                             .findFirst()
                             .map(TicketMessage::getContent)
                             .orElse(StringUtils.EMPTY);
    }

    private String getLatestMessage(
        List<TicketMessage> ticketMessages,
        Predicate<TicketMessage> selector,
        String fallback
    ) {
        for (int i = ticketMessages.size() - 1; i >= 0; i--) {
            TicketMessage ticketMessage = ticketMessages.get(i);
            if (ticketMessage == null || StringUtils.isBlank(ticketMessage.getContent())) {
                continue;
            }

            if (selector.test(ticketMessage)) {
                return ticketMessage.getContent();
            }
        }

        return StringUtils.defaultString(fallback);
    }

    private boolean isAssistantMessage(
        TicketMessage ticketMessage
    ) {
        if (ticketMessage.getAuthor() == null || ticketMessage.getAuthor().getId() == null) {
            return true;
        }

        return !ticketMessage.getAuthor().isCustomer();
    }

    private boolean isCustomerMessage(
        TicketMessage ticketMessage
    ) {
        return ticketMessage.getAuthor() != null
               && ticketMessage.getAuthor().isCustomer();
    }

    private String buildConversationText(
        SupportTicket supportTicket,
        List<TicketMessage> ticketMessages
    ) {
        int messageCount = ticketMessages.size();
        int startIndex = Math.max(0, messageCount - MAX_CONVERSATION_MESSAGES);
        List<TicketMessage> recentMessages = ticketMessages.subList(startIndex, messageCount);

        String ticketConversation = recentMessages.stream()
                                                  .map(
                                                      message -> String.format(
                                                          "[%s] %s: %s",
                                                          message.getCreatedAt(),
                                                          message.getMessageKind(),
                                                          message.getContent()
                                                      )
                                                  )
                                                  .collect(Collectors.joining("\n"));

        String customerContext = customerContextEnrichmentService.buildCustomerContext(
            supportTicket.getCustomer().getId(),
            supportTicket.getId()
        );

        if (StringUtils.isBlank(customerContext)) {
            return ticketConversation;
        }

        return String.join(
            "\n\n",
            ticketConversation,
            customerContext
        );
    }
}
