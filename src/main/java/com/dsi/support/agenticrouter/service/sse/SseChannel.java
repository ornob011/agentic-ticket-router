package com.dsi.support.agenticrouter.service.sse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseChannel {
    ROUTING_PROGRESS("routing.progress"),
    DRAFT_REPLY("draft.reply");

    private final String code;
}
