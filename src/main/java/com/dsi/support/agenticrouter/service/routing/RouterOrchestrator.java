package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.configuration.RagConfiguration;
import com.dsi.support.agenticrouter.dto.*;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketAutonomousMetadata;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.service.analysis.TicketAnalysisService;
import com.dsi.support.agenticrouter.service.knowledge.KnowledgeRetrievalService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    private static final int MIN_QUERY_LENGTH = 10;
    private static final String NO_PREVIOUS_QUESTION = "None";

    private final TicketRouterService ticketRouterService;
    private final PolicyEngine policyEngine;
    private final AgenticStateMachine agenticStateMachine;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAnalysisService ticketAnalysisService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final RagConfiguration ragConfiguration;

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

        RouterResponse routerResponse = ticketRouterService.getRoutingDecision(
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
            initialMessage,
            ticketAnalysisResult.getCategory()
        );

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
                            .questionsAsked(questionsAsked)
                            .build();
    }

    private List<ArticleSearchResult> searchRelevantArticles(
        String subject,
        String initialMessage,
        TicketCategory categoryHint
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

        List<ArticleSearchResult> relevantArticles = knowledgeRetrievalService.retrieveRelevantArticles(
            vectorQuery,
            categoryHint,
            ragConfiguration.getReturnTopK()
        );

        log.debug(
            "RelevantArticleSearch({}) Outcome(queryLength:{},topK:{},resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            vectorQuery.length(),
            ragConfiguration.getReturnTopK(),
            relevantArticles.size()
        );

        return relevantArticles;
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
