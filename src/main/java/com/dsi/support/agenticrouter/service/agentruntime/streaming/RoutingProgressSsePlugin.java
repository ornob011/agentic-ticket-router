package com.dsi.support.agenticrouter.service.agentruntime.streaming;

import com.dsi.support.agenticrouter.service.sse.SseChannel;
import com.dsi.support.agenticrouter.service.sse.SseChannelPlugin;
import com.dsi.support.agenticrouter.service.sse.SseSession;
import org.springframework.stereotype.Component;

@Component
public class RoutingProgressSsePlugin implements SseChannelPlugin<Long> {

    @Override
    public SseChannel channel() {
        return SseChannel.ROUTING_PROGRESS;
    }

    @Override
    public Class<Long> contextType() {
        return Long.class;
    }

    @Override
    public String resourceId(
        Long context
    ) {
        return String.valueOf(context);
    }

    @Override
    public long emitterTimeoutMs(
        Long context
    ) {
        return 0L;
    }

    @Override
    public long heartbeatIntervalSeconds(
        Long context
    ) {
        return 15L;
    }

    @Override
    public void onSubscribe(
        SseSession session,
        Long context
    ) {
    }
}
