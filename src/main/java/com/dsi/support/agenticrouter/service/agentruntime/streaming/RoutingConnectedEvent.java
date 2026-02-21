package com.dsi.support.agenticrouter.service.agentruntime.streaming;

import java.time.Instant;

public record RoutingConnectedEvent(
    Long ticketId,
    Instant timestamp
) {

    public static RoutingConnectedEvent now(
        Long ticketId
    ) {
        return new RoutingConnectedEvent(
            ticketId,
            Instant.now()
        );
    }
}
