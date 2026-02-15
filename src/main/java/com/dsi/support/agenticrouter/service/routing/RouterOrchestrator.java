package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.dto.TicketAnalysisRequest;
import com.dsi.support.agenticrouter.dto.TicketAnalysisResult;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.analysis.TicketAnalysisService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouterOrchestrator {

    private final TicketRouterService ticketRouterService;
    private final PolicyEngine policyEngine;
    private final AgenticStateMachine agenticStateMachine;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketAnalysisService ticketAnalysisService;
    private final RoutingRequestFactory routingRequestFactory;

    @Async("ticketRoutingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void routeTicket(
        Long ticketId
    ) throws BindException {
        log.info(
            "TicketRoute({}) SupportTicket(id:{})",
            OperationalLogContext.PHASE_START,
            ticketId
        );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
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

    private RouterRequest buildRouterRequest(
        SupportTicket supportTicket,
        TicketAnalysisResult ticketAnalysisResult
    ) {
        return routingRequestFactory.buildRouterRequest(
            supportTicket,
            ticketAnalysisResult
        );
    }

    private TicketAnalysisRequest buildAnalysisRequest(
        SupportTicket supportTicket
    ) {
        return routingRequestFactory.buildAnalysisRequest(
            supportTicket
        );
    }
}
