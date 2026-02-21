package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.exception.RoutingExecutionException;
import lombok.RequiredArgsConstructor;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingExecutionCoordinator {

    private final List<RoutingExecutionStrategy> routingExecutionStrategies;

    public RoutingExecutionResult execute(
        SupportTicket supportTicket,
        RouterRequest routerRequest
    ) throws BindException, GraphStateException {
        for (RoutingExecutionStrategy routingExecutionStrategy : routingExecutionStrategies) {
            if (!routingExecutionStrategy.supports(supportTicket)) {
                continue;
            }

            RoutingExecutionResult result = routingExecutionStrategy.execute(
                supportTicket,
                routerRequest
            );

            if (result.terminal()) {
                return result;
            }
        }

        throw new RoutingExecutionException("No terminal routing strategy executed");
    }
}
