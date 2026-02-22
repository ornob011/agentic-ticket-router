package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.service.sse.SseChannel;
import com.dsi.support.agenticrouter.service.sse.SseEngine;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class DraftReplyStreamController {

    private final SseEngine sseEngine;

    @GetMapping("/{ticketId}/draft/stream")
    @PreAuthorize("@ticketAuthorizationService.canAccessTicket(#ticketId)")
    public SseEmitter streamDraftReply(
        @PathVariable Long ticketId
    ) {
        log.info(
            "DraftReplyStream({}) SupportTicket(id:{}) Outcome(start)",
            OperationalLogContext.PHASE_START,
            ticketId
        );

        return sseEngine.subscribe(
            SseChannel.DRAFT_REPLY,
            ticketId
        );
    }
}
