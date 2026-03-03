package com.dsi.support.agenticrouter.service.sse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SseSession {

    private final SseEngine sseEngine;
    private final SseChannel channel;
    private final String resourceId;

    public void publish(
        SseEventType eventType,
        Object payload
    ) {
        sseEngine.publish(
            channel,
            resourceId,
            eventType,
            payload
        );
    }

    public void complete(
        Object payload
    ) {
        sseEngine.complete(
            channel,
            resourceId,
            payload
        );
    }

    public void error(
        Object payload
    ) {
        sseEngine.error(
            channel,
            resourceId,
            payload
        );
    }
}
