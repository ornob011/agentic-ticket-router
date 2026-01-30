package com.dsi.support.agenticrouter.resource.routing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class TicketRoutingPrompt extends RoutingPromptResource {

    @Value("classpath:/prompts/ticket-routing.st")
    private Resource routingPrompt;

    @Override
    public Resource getRoutingPrompt() {
        return routingPrompt;
    }
}
