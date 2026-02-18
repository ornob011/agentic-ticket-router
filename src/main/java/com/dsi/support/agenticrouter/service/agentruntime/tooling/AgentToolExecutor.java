package com.dsi.support.agenticrouter.service.agentruntime.tooling;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.service.routing.AgenticStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

@Service
@RequiredArgsConstructor
public class AgentToolExecutor {

    private final AgenticStateMachine agenticStateMachine;

    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        agenticStateMachine.executeAction(
            supportTicket,
            routerResponse
        );
    }
}
