package com.dsi.support.agenticrouter.service.sse;

public interface SseChannelPlugin<C> {

    SseChannel channel();

    Class<C> contextType();

    String resourceId(C context);

    default long emitterTimeoutMs(
        C context
    ) {
        return 0L;
    }

    default long heartbeatIntervalSeconds(
        C context
    ) {
        return 15L;
    }

    void onSubscribe(
        SseSession session,
        C context
    );
}
