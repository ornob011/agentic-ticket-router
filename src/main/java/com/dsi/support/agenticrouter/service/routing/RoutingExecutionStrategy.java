package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.validation.BindException;

public interface RoutingExecutionStrategy {

    boolean supports(
        SupportTicket supportTicket
    );

    RoutingExecutionResult execute(
        SupportTicket supportTicket,
        RouterRequest routerRequest
    ) throws BindException, GraphStateException;
}
