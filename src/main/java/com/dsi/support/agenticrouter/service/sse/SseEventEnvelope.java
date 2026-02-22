package com.dsi.support.agenticrouter.service.sse;

import java.time.Instant;

public record SseEventEnvelope(
    SseChannel channel,
    SseEventName eventName,
    SseEventType eventType,
    String streamId,
    String resourceId,
    Instant timestamp,
    Object payload
) {

    public static SseEventEnvelope of(
        SseChannel channel,
        SseEventName eventName,
        SseEventType eventType,
        String streamId,
        String resourceId,
        Object payload
    ) {
        return new SseEventEnvelope(
            channel,
            eventName,
            eventType,
            streamId,
            resourceId,
            Instant.now(),
            payload
        );
    }
}
