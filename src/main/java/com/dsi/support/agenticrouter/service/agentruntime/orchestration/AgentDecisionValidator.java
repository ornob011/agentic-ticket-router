package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AgentDecisionValidator {

    public void validate(
        RouterResponse routerResponse
    ) {
        Objects.requireNonNull(routerResponse, "routerResponse");
        Objects.requireNonNull(routerResponse.getNextAction(), "routerResponse.nextAction");
        Objects.requireNonNull(routerResponse.getQueue(), "routerResponse.queue");
        Objects.requireNonNull(routerResponse.getPriority(), "routerResponse.priority");
        Objects.requireNonNull(routerResponse.getConfidence(), "routerResponse.confidence");
    }
}
