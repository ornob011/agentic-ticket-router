package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.service.agentruntime.streaming.StreamingDraftService;
import com.dsi.support.agenticrouter.service.ticket.TicketCommandLookupService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class DraftReplyStreamController {

    private static final long EMITTER_TIMEOUT_MS = 60_000L;

    private final TicketCommandLookupService ticketCommandLookupService;
    private final StreamingDraftService streamingDraftService;

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

        SupportTicket ticket = ticketCommandLookupService.requireTicket(
            ticketId
        );

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        Schedulers.boundedElastic().schedule(() ->
            streamingDraftService.streamDraftReply(ticket)
                                 .doOnNext(token -> {
                                     try {
                                         emitter.send(SseEmitter.event().data(" " + token));
                                     } catch (IOException e) {
                                         emitter.completeWithError(e);
                                     }
                                 })
                                 .doOnComplete(() -> {
                                     try {
                                         emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                         emitter.complete();
                                     } catch (IOException e) {
                                         emitter.completeWithError(e);
                                     }
                                 })
                                 .doOnError(e -> {
                                     log.error(
                                         "DraftReplyStream({}) SupportTicket(id:{}) Outcome(reason:{})",
                                         OperationalLogContext.PHASE_FAIL,
                                         ticketId,
                                         "stream_failed",
                                         e
                                     );
                                     emitter.completeWithError(e);
                                 })
                                 .doOnTerminate(() -> log.info(
                                     "DraftReplyStream({}) SupportTicket(id:{}) Outcome(completed)",
                                     OperationalLogContext.PHASE_COMPLETE,
                                     ticketId
                                 ))
                                 .subscribe()
        );

        return emitter;
    }
}
