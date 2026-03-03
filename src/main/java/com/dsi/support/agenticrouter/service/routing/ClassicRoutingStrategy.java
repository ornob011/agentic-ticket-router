package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.RoutingSource;
import lombok.RequiredArgsConstructor;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

@Service
@Order(2)
@RequiredArgsConstructor
public class ClassicRoutingStrategy implements RoutingExecutionStrategy {

    private final TicketRouterService ticketRouterService;
    private final PolicyEngine policyEngine;
    private final AgenticStateMachine agenticStateMachine;

    @Override
    public boolean supports(
        SupportTicket supportTicket
    ) {
        return true;
    }

    @Override
    public RoutingExecutionResult execute(
        SupportTicket supportTicket,
        RouterRequest routerRequest
    ) throws BindException, GraphStateException {
        RouterResponse routerResponse = ticketRouterService.getRoutingDecision(
            routerRequest,
            supportTicket.getId()
        );

        routerResponse = policyEngine.applyPolicyGates(
            routerResponse
        );

        agenticStateMachine.executeAction(
            supportTicket,
            routerResponse
        );

        return new RoutingExecutionResult(
            true,
            RoutingSource.CLASSIC,
            routerResponse
        );
    }
}
