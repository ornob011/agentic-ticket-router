package com.dsi.support.agenticrouter.service.agentruntime.streaming;

import com.dsi.support.agenticrouter.enums.AgentNodeStatus;
import com.dsi.support.agenticrouter.enums.AgentRuntimeGraphNode;

import java.time.Instant;

public record AgentProgressEvent(
    Long ticketId,
    AgentRuntimeGraphNode node,
    AgentNodeStatus status,
    String message,
    Instant timestamp
) {

    public static AgentProgressEvent started(
        Long ticketId,
        AgentRuntimeGraphNode node,
        String message
    ) {
        return new AgentProgressEvent(
            ticketId,
            node,
            AgentNodeStatus.STARTED,
            message,
            Instant.now()
        );
    }

    public static AgentProgressEvent completed(
        Long ticketId,
        AgentRuntimeGraphNode node,
        String message
    ) {
        return new AgentProgressEvent(
            ticketId,
            node,
            AgentNodeStatus.COMPLETED,
            message,
            Instant.now()
        );
    }
}
