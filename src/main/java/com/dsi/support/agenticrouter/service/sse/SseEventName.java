package com.dsi.support.agenticrouter.service.sse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventName {
    CONNECTED("sse-connected"),
    HEARTBEAT("sse-heartbeat"),
    EVENT("sse-event"),
    COMPLETE("sse-complete"),
    ERROR("sse-error");

    private final String value;
}
