package com.dsi.support.agenticrouter.service.sse;

public enum SseFailureReason {
    CONNECTED_EMIT_FAILED,
    HEARTBEAT_EMIT_FAILED,
    EVENT_EMIT_FAILED,
    COMPLETE_EMIT_FAILED,
    ERROR_EMIT_FAILED
}
