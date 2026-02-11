package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterRequest;
import com.fasterxml.jackson.databind.JsonNode;

public interface RoutingModelClient {

    JsonNode requestRoutingDecision(
        RouterRequest routerRequest
    );
}
