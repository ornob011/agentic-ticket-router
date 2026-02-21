package com.dsi.support.agenticrouter.service.agentruntime.streaming;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterRegistry {

    private static final long EMITTER_TIMEOUT_MS = 30_000L;

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long ticketId) {
        log.debug(
            "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(register)",
            OperationalLogContext.PHASE_START,
            ticketId
        );

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        SseEmitter existingEmitter = emitters.put(ticketId, emitter);

        if (existingEmitter != null) {
            existingEmitter.complete();
        }

        emitter.onCompletion(() -> {
            emitters.remove(ticketId);
            log.debug(
                "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(removed:completion)",
                OperationalLogContext.PHASE_COMPLETE,
                ticketId
            );
        });
        emitter.onTimeout(() -> {
            emitters.remove(ticketId);
            log.debug(
                "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(removed:timeout)",
                OperationalLogContext.PHASE_FAIL,
                ticketId
            );
        });
        emitter.onError(error -> {
            emitters.remove(ticketId);
            log.debug(
                "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(removed:error)",
                OperationalLogContext.PHASE_FAIL,
                ticketId,
                error
            );
        });

        try {
            emitter.send(
                SseEmitter.event()
                          .name("routing-connected")
                          .data(RoutingConnectedEvent.now(ticketId))
            );
            log.debug(
                "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(connected_event_sent)",
                OperationalLogContext.PHASE_COMPLETE,
                ticketId
            );
        } catch (IOException exception) {
            emitters.remove(ticketId);
            log.debug(
                "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                ticketId,
                "connected_emit_failed",
                exception
            );
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void emit(Long ticketId, AgentProgressEvent event) {
        SseEmitter emitter = emitters.get(ticketId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(
                SseEmitter.event()
                          .name("agent-progress")
                          .data(event)
            );
        } catch (IOException exception) {
            log.debug(
                "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                ticketId,
                "emit_failed",
                exception
            );
            emitters.remove(ticketId);
        }
    }

    public void complete(
        Long ticketId
    ) {
        SseEmitter emitter = emitters.remove(ticketId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(
                SseEmitter.event()
                          .name("routing-complete")
                          .data("done")
            );
        } catch (IOException exception) {
            log.debug(
                "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                ticketId,
                "complete_emit_failed",
                exception
            );
        }

        emitter.complete();
        log.debug(
            "SseEmitterRegistry({}) SupportTicket(id:{}) Outcome(completed)",
            OperationalLogContext.PHASE_COMPLETE,
            ticketId
        );
    }
}
