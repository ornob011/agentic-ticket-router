package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.configuration.AgentRuntimeConfiguration;
import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.RoutingSource;
import com.dsi.support.agenticrouter.service.agentruntime.AgentRuntimeEligibilityService;
import com.dsi.support.agenticrouter.service.agentruntime.AgentRuntimeOrchestrator;
import lombok.RequiredArgsConstructor;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

@Service
@Order(1)
@RequiredArgsConstructor
public class AgentRuntimeRoutingStrategy implements RoutingExecutionStrategy {

    private final AgentRuntimeEligibilityService agentRuntimeEligibilityService;
    private final AgentRuntimeConfiguration agentRuntimeConfiguration;
    private final AgentRuntimeOrchestrator agentRuntimeOrchestrator;

    @Override
    public boolean supports(
        SupportTicket supportTicket
    ) {
        return agentRuntimeEligibilityService.isEligible(
            supportTicket
        );
    }

    @Override
    public RoutingExecutionResult execute(
        SupportTicket supportTicket,
        RouterRequest routerRequest
    ) throws BindException, GraphStateException {
        RouterResponse runtimeResponse = agentRuntimeOrchestrator.execute(
            supportTicket,
            routerRequest
        );

        if (!agentRuntimeConfiguration.isShadowMode()) {
            return new RoutingExecutionResult(
                true,
                RoutingSource.AGENT_RUNTIME,
                runtimeResponse
            );
        }

        return new RoutingExecutionResult(
            false,
            RoutingSource.AGENT_RUNTIME_SHADOW,
            runtimeResponse
        );
    }
}
