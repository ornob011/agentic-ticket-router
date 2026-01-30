package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.dto.TicketAnalysisRequest;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.TicketAnalysis;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.repository.TicketRoutingRepository;
import com.dsi.support.agenticrouter.service.analysis.TicketAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RouterOrchestrator {

    private final OllamaRouterService ollamaRouterService;
    private final PolicyEngine policyEngine;
    private final AgenticStateMachine agenticStateMachine;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketRoutingRepository ticketRoutingRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAnalysisService ticketAnalysisService;

    @Async
    public void routeTicket(
        SupportTicket supportTicket
    ) {
        supportTicket.setStatus(TicketStatus.TRIAGING);
        supportTicket = supportTicketRepository.save(supportTicket);

        TicketAnalysisRequest analysisRequest = buildAnalysisRequest(
            supportTicket
        );

        runTicketAnalysis(
            analysisRequest
        );

        RouterRequest routerRequest = buildRouterRequest(
            supportTicket
        );

        RouterResponse routerResponse = ollamaRouterService.getRoutingDecision(
            routerRequest,
            supportTicket.getId()
        );

        routerResponse = policyEngine.applyPolicyGates(
            routerResponse
        );

        int newVersion = supportTicket.getLatestRoutingVersion() + 1;

        TicketRouting routing = createRoutingRecord(
            supportTicket,
            routerResponse,
            newVersion
        );

        routing = ticketRoutingRepository.save(routing);

        supportTicket.setCurrentCategory(routerResponse.getCategory());
        supportTicket.setCurrentPriority(routerResponse.getPriority());
        supportTicket.setAssignedQueue(routerResponse.getQueue());
        supportTicket.setLatestRoutingConfidence(routerResponse.getConfidence());
        supportTicket.setLatestRoutingVersion(newVersion);
        supportTicket = supportTicketRepository.save(supportTicket);

        agenticStateMachine.executeAction(
            supportTicket,
            routing
        );
    }

    private void runTicketAnalysis(
        TicketAnalysisRequest analysisRequest
    ) {
        ticketAnalysisService.analyzeTicketSection(
            analysisRequest,
            TicketAnalysis.CUSTOMER_INFORMATION
        );

        ticketAnalysisService.analyzeTicketSection(
            analysisRequest,
            TicketAnalysis.CONVERSATION_HISTORY
        );

        ticketAnalysisService.analyzeTicketSection(
            analysisRequest,
            TicketAnalysis.TECHNICAL_DETAILS
        );

        ticketAnalysisService.analyzeTicketSection(
            analysisRequest,
            TicketAnalysis.ACTIONS_REQUIRED
        );
    }

    private TicketAnalysisRequest buildAnalysisRequest(
        SupportTicket supportTicket
    ) {
        String content = String.format(
            "Ticket Number: %s\nSubject: %s\nCustomer: %s\nCustomer Tier: %s\n\nConversation:\n%s",
            supportTicket.getFormattedTicketNo(),
            supportTicket.getSubject(),
            supportTicket.getCustomer().getFullName(),
            supportTicket.getCustomer().getCustomerProfile().getCustomerTier().getCode(),
            buildConversationText(supportTicket)
        );

        return TicketAnalysisRequest.builder()
                                    .ticketId(supportTicket.getId())
                                    .content(content)
                                    .build();
    }

    private RouterRequest buildRouterRequest(
        SupportTicket supportTicket
    ) {
        String conversationHistory = buildConversationText(supportTicket);

        String initialMessage = ticketMessageRepository.findByTicket_IdOrderByCreatedAtAsc(supportTicket.getId())
                                                       .stream()
                                                       .findFirst()
                                                       .map(TicketMessage::getContent)
                                                       .orElse(StringUtils.EMPTY);

        String customerTier = supportTicket.getCustomer()
                                           .getCustomerProfile()
                                           .getCustomerTier()
                                           .getCode();

        return RouterRequest.builder()
                            .ticketId(supportTicket.getId())
                            .ticketNo(supportTicket.getFormattedTicketNo())
                            .subject(supportTicket.getSubject())
                            .customerName(supportTicket.getCustomer().getFullName())
                            .customerTier(customerTier)
                            .initialMessage(initialMessage)
                            .conversationHistory(conversationHistory)
                            .customerInfoAnalysis("See ticket analysis results")
                            .conversationAnalysis("See ticket analysis results")
                            .technicalAnalysis("See ticket analysis results")
                            .actionsAnalysis("See ticket analysis results")
                            .build();
    }

    private String buildConversationText(SupportTicket supportTicket) {
        return ticketMessageRepository.findByTicket_IdOrderByCreatedAtAsc(supportTicket.getId())
                                      .stream()
                                      .filter(TicketMessage::isVisibleToCustomer)
                                      .map(
                                          message ->
                                              String.format("[%s] %s: %s",
                                                  message.getCreatedAt(),
                                                  message.getMessageKind(),
                                                  message.getContent()
                                              )
                                      )
                                      .collect(Collectors.joining("\n"));
    }

    private TicketRouting createRoutingRecord(
        SupportTicket supportTicket,
        RouterResponse routerResponse,
        int version
    ) {
        return TicketRouting.builder()
                            .ticket(supportTicket)
                            .version(version)
                            .category(routerResponse.getCategory())
                            .priority(routerResponse.getPriority())
                            .queue(routerResponse.getQueue())
                            .nextAction(routerResponse.getNextAction())
                            .confidence(routerResponse.getConfidence())
                            .clarifyingQuestion(routerResponse.getClarifyingQuestion())
                            .draftReply(routerResponse.getDraftReply())
                            .rationaleTags(routerResponse.getRationaleTags())
                            .overridden(false)
                            .policyGateTriggered(false)
                            .applied(true)
                            .build();
    }
}
