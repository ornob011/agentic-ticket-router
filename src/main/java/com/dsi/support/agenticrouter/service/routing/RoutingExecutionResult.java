package com.dsi.support.agenticrouter.service.routing;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.RoutingSource;

public record RoutingExecutionResult(
    boolean terminal,
    RoutingSource source,
    RouterResponse response
) {
}
