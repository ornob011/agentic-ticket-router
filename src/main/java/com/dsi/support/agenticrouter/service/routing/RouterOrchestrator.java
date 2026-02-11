package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.*;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketAutonomousMetadata;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.enums.VectorStoreMetadataKey;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.analysis.TicketAnalysisService;
import com.dsi.support.agenticrouter.service.knowledge.KnowledgeBaseVectorStore;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouterOrchestrator {

    private static final int TOP_K_ARTICLES = 5;
    private static final double ARTICLE_SIMILARITY_THRESHOLD = 0.75;
    private static final int MIN_QUERY_LENGTH = 10;

    private static final int MAX_AUTONOMOUS_ACTIONS = 5;
    private static final int MAX_QUESTIONS = 3;

    private static final String TITLE_SEPARATOR = System.lineSeparator() + System.lineSeparator();
    private static final String UNKNOWN_TITLE = "Unknown Title";
    private static final String NO_PREVIOUS_QUESTION = "None";

    private final OllamaRouterService ollamaRouterService;
    private final PolicyEngine policyEngine;
    private final AgenticStateMachine agenticStateMachine;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAnalysisService ticketAnalysisService;
    private final KnowledgeBaseVectorStore knowledgeBaseVectorStore;

    @Async("ticketRoutingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void routeTicket(
        Long ticketId
    ) {
        log.info(
            "TicketRoute({}) SupportTicket(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId
        );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 () -> new DataNotFoundException(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        supportTicket.setStatus(TicketStatus.TRIAGING);
        supportTicket = supportTicketRepository.save(supportTicket);

        log.info(
            "TicketRoute({}) SupportTicket(id:{},ticketNo:{},status:{})",
            OperationalLogContext.PHASE_PERSIST,
            supportTicket.getId(),
            supportTicket.getFormattedTicketNo(),
            supportTicket.getStatus()
        );

        TicketAnalysisRequest analysisRequest = buildAnalysisRequest(
            supportTicket
        );

        TicketAnalysisResult analysisResult = ticketAnalysisService.analyzeTicket(
            analysisRequest
        );

        log.debug(
            "TicketRoute({}) SupportTicket(id:{},status:{}) Outcome(analysisCategory:{},analysisLength:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            analysisResult.getCategory(),
            StringUtils.length(analysisResult.getAnalysis())
        );

        RouterRequest routerRequest = buildRouterRequest(
            supportTicket,
            analysisResult
        );

        RouterResponse routerResponse = ollamaRouterService.getRoutingDecision(
            routerRequest,
            supportTicket.getId()
        );

        log.info(
            "TicketRoute({}) SupportTicket(id:{},status:{}) RouterResponse(nextAction:{},queue:{},priority:{},confidence:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routerResponse.getNextAction(),
            routerResponse.getQueue(),
            routerResponse.getPriority(),
            routerResponse.getConfidence()
        );

        routerResponse = policyEngine.applyPolicyGates(
            routerResponse
        );

        log.info(
            "TicketRoute({}) SupportTicket(id:{},status:{}) RouterResponse(nextAction:{},queue:{},priority:{},confidence:{})",
            OperationalLogContext.PHASE_DECISION,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routerResponse.getNextAction(),
            routerResponse.getQueue(),
            routerResponse.getPriority(),
            routerResponse.getConfidence()
        );

        agenticStateMachine.executeAction(
            supportTicket,
            routerResponse
        );

        log.info(
            "TicketRoute({}) SupportTicket(id:{},status:{}) Outcome(nextAction:{})",
            OperationalLogContext.PHASE_COMPLETE,
            supportTicket.getId(),
            supportTicket.getStatus(),
            routerResponse.getNextAction()
        );
    }

    private TicketAnalysisRequest buildAnalysisRequest(
        SupportTicket supportTicket
    ) {
        String conversationText = buildConversationText(
            supportTicket
        );

        String analysisContent = String.format(
            "Ticket Number: %s%n" +
            "Subject: %s%n" +
            "Customer: %s%n" +
            "Customer Tier: %s%n%n" +
            "Conversation:%n%s",
            supportTicket.getFormattedTicketNo(),
            supportTicket.getSubject(),
            supportTicket.getCustomer().getFullName(),
            supportTicket.getCustomer().getCustomerProfile().getCustomerTier().getCode(),
            conversationText
        );

        return TicketAnalysisRequest.builder()
                                    .ticketId(supportTicket.getId())
                                    .content(analysisContent)
                                    .build();
    }

    private RouterRequest buildRouterRequest(
        SupportTicket supportTicket,
        TicketAnalysisResult ticketAnalysisResult
    ) {
        String conversationHistory = buildConversationText(
            supportTicket
        );

        String initialMessage = getInitialMessage(
            supportTicket
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

        List<ArticleSearchResult> relevantArticles = searchRelevantArticles(
            supportTicket.getSubject(),
            initialMessage
        );

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
                            .analysis(StringUtils.defaultString(ticketAnalysisResult.getAnalysis()))
                            .suggestedCategory(ticketAnalysisResult.getCategory())
                            .previousClarifyingQuestion(lastClarifyingQuestion)
                            .relevantArticles(relevantArticles)
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
                " ",
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
                       .map(StringUtils::trimToNull);
    }

    private Optional<Long> parseLong(
        String numericText
    ) {
        return Optional.ofNullable(StringUtils.trimToNull(numericText))
                       .filter(StringUtils::isNumeric)
                       .map(Long::valueOf);
    }

    private Optional<Integer> parseInt(
        String numericText
    ) {
        return Optional.ofNullable(StringUtils.trimToNull(numericText))
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
                       .map(parts -> StringUtils.trimToNull(parts[0]))
                       .orElse(UNKNOWN_TITLE);
    }

    private String getInitialMessage(
        SupportTicket supportTicket
    ) {
        return ticketMessageRepository.findByTicketIdWithAuthorOrderByCreatedAtAsc(
                                          supportTicket.getId()
                                      )
                                      .stream()
                                      .findFirst()
                                      .map(TicketMessage::getContent)
                                      .orElse(StringUtils.EMPTY);
    }

    private String buildConversationText(
        SupportTicket supportTicket
    ) {
        return ticketMessageRepository.findByTicketIdWithAuthorOrderByCreatedAtAsc(
                                          supportTicket.getId()
                                      )
                                      .stream()
                                      .map(
                                          message -> String.format(
                                              "[%s] %s: %s",
                                              message.getCreatedAt(),
                                              message.getMessageKind(),
                                              message.getContent()
                                          )
                                      )
                                      .collect(Collectors.joining("\n"));
    }
}
