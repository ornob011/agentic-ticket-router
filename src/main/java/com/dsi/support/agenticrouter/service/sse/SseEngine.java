package com.dsi.support.agenticrouter.service.sse;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DisconnectedClientHelper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseEngine {

    private final List<SseChannelPlugin<?>> plugins;
    private final DisconnectedClientHelper disconnectedClientHelper = new DisconnectedClientHelper(SseEngine.class.getName());

    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("routing-sse-heartbeat-1");
        thread.setDaemon(true);
        return thread;
    });

    public SseEmitter subscribe(
        SseChannel channel,
        Object context
    ) {
        SseChannelPlugin<Object> plugin = resolvePlugin(
            channel,
            context
        );

        String resourceId = plugin.resourceId(
            context
        );

        String streamId = streamId(
            channel,
            resourceId
        );

        log.debug(
            "SseEngine({}) SseChannel(channel:{},resourceId:{}) Outcome(subscribe)",
            OperationalLogContext.PHASE_START,
            channel,
            resourceId
        );

        SseEmitter emitter = new SseEmitter(
            plugin.emitterTimeoutMs(context)
        );

        SessionState existing = sessions.put(
            streamId,
            new SessionState(
                channel,
                resourceId,
                emitter,
                null
            )
        );

        if (Objects.nonNull(existing)) {
            cancelHeartbeat(existing);
            safeComplete(existing.emitter());
            log.debug(
                "SseEngine({}) SseChannel(channel:{},resourceId:{}) Outcome(replaced_existing_session)",
                OperationalLogContext.PHASE_COMPLETE,
                channel,
                resourceId
            );
        }

        emitter.onCompletion(() ->
            removeSession(
                streamId,
                emitter
            )
        );
        emitter.onTimeout(() ->
            removeSession(
                streamId,
                emitter
            )
        );
        emitter.onError(error ->
            removeSession(
                streamId,
                emitter
            )
        );

        sendConnected(
            streamId,
            channel,
            resourceId,
            emitter
        );

        SessionState registered = sessions.get(streamId);
        if (Objects.nonNull(registered) && registered.emitter() == emitter) {
            ScheduledFuture<?> heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate(
                () -> sendHeartbeat(
                    streamId,
                    channel,
                    resourceId,
                    emitter
                ),
                plugin.heartbeatIntervalSeconds(context),
                plugin.heartbeatIntervalSeconds(context),
                TimeUnit.SECONDS
            );
            sessions.computeIfPresent(
                streamId,
                (key, state) -> new SessionState(
                    state.channel(),
                    state.resourceId(),
                    state.emitter(),
                    heartbeatFuture
                )
            );
        }

        plugin.onSubscribe(
            new SseSession(
                this,
                channel,
                resourceId
            ),
            context
        );

        return emitter;
    }

    public void publish(
        SseChannel channel,
        String resourceId,
        SseEventType eventType,
        Object payload
    ) {
        String streamId = streamId(
            channel,
            resourceId
        );

        SessionState state = sessions.get(
            streamId
        );

        if (Objects.isNull(state)) {
            return;
        }

        sendEnvelope(
            streamId,
            state,
            SseEventName.EVENT,
            eventType,
            payload,
            SseFailureReason.EVENT_EMIT_FAILED
        );
    }

    public void complete(
        SseChannel channel,
        String resourceId,
        Object payload
    ) {
        String streamId = streamId(
            channel,
            resourceId
        );

        SessionState state = sessions.remove(
            streamId
        );

        if (Objects.isNull(state)) {
            return;
        }

        cancelHeartbeat(
            state
        );

        sendEnvelope(
            streamId,
            state,
            SseEventName.COMPLETE,
            SseEventType.COMPLETE,
            payload,
            SseFailureReason.COMPLETE_EMIT_FAILED
        );

        safeComplete(
            state.emitter()
        );
    }

    public void error(
        SseChannel channel,
        String resourceId,
        Object payload
    ) {
        String streamId = streamId(
            channel,
            resourceId
        );

        SessionState state = sessions.remove(
            streamId
        );

        if (Objects.isNull(state)) {
            return;
        }

        cancelHeartbeat(
            state
        );

        sendEnvelope(
            streamId,
            state,
            SseEventName.ERROR,
            SseEventType.ERROR,
            payload,
            SseFailureReason.ERROR_EMIT_FAILED
        );

        safeComplete(state.emitter());
    }

    @PreDestroy
    public void shutdown() {
        heartbeatScheduler.shutdown();
        try {
            if (!heartbeatScheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            heartbeatScheduler.shutdownNow();
        }
    }

    private void sendConnected(
        String streamId,
        SseChannel channel,
        String resourceId,
        SseEmitter emitter
    ) {
        SessionState state = sessions.get(streamId);

        if (Objects.isNull(state) || state.emitter() != emitter) {
            return;
        }

        sendEnvelope(
            streamId,
            state,
            SseEventName.CONNECTED,
            SseEventType.CONNECTED,
            null,
            SseFailureReason.CONNECTED_EMIT_FAILED
        );
    }

    private void sendHeartbeat(
        String streamId,
        SseChannel channel,
        String resourceId,
        SseEmitter emitter
    ) {
        SessionState state = sessions.get(streamId);

        if (Objects.isNull(state) || state.emitter() != emitter) {
            return;
        }

        sendEnvelope(
            streamId,
            state,
            SseEventName.HEARTBEAT,
            SseEventType.HEARTBEAT,
            null,
            SseFailureReason.HEARTBEAT_EMIT_FAILED
        );
    }

    private void sendEnvelope(
        String streamId,
        SessionState sessionState,
        SseEventName eventName,
        SseEventType eventType,
        Object payload,
        SseFailureReason failureReason
    ) {
        try {
            SseEventEnvelope envelope = SseEventEnvelope.of(
                sessionState.channel(),
                eventName,
                eventType,
                streamId,
                sessionState.resourceId(),
                payload
            );

            sessionState.emitter().send(
                SseEmitter.event()
                          .name(eventName.getValue())
                          .data(envelope)
            );
        } catch (Exception exception) {
            sessions.remove(streamId, sessionState);
            cancelHeartbeat(sessionState);
            safeComplete(sessionState.emitter());

            if (disconnectedClientHelper.checkAndLogClientDisconnectedException(exception)) {
                return;
            }

            log.warn(
                "SseEngine({}) SseChannel(channel:{},resourceId:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                sessionState.channel(),
                sessionState.resourceId(),
                failureReason,
                exception
            );
        }
    }

    private void removeSession(
        String streamId,
        SseEmitter emitter
    ) {
        sessions.computeIfPresent(
            streamId,
            (key, state) -> {
                if (state.emitter() != emitter) {
                    return state;
                }

                cancelHeartbeat(state);
                log.debug(
                    "SseEngine({}) SseChannel(channel:{},resourceId:{}) Outcome(removed_session)",
                    OperationalLogContext.PHASE_COMPLETE,
                    state.channel(),
                    state.resourceId()
                );
                return null;
            }
        );
    }

    private void cancelHeartbeat(
        SessionState sessionState
    ) {
        if (Objects.isNull(sessionState.heartbeatFuture())) {
            return;
        }
        sessionState.heartbeatFuture().cancel(true);
    }

    private void safeComplete(
        SseEmitter emitter
    ) {
        try {
            emitter.complete();
        } catch (Exception exception) {
            log.debug(
                "SseEngine({}) Outcome(reason:safe_complete_failed)",
                OperationalLogContext.PHASE_FAIL
            );
        }
    }

    @SuppressWarnings("unchecked")
    private SseChannelPlugin<Object> resolvePlugin(
        SseChannel channel,
        Object context
    ) {
        Map<SseChannel, SseChannelPlugin<?>> pluginMap = plugins.stream()
                                                                .collect(
                                                                    Collectors.toMap(
                                                                        SseChannelPlugin::channel,
                                                                        Function.identity()
                                                                    )
                                                                );

        SseChannelPlugin<?> plugin = pluginMap.get(channel);
        if (Objects.isNull(plugin)) {
            throw new IllegalArgumentException("sse.channel.not.supported:" + channel.name());
        }

        if (!plugin.contextType().isInstance(context)) {
            throw new IllegalArgumentException("sse.channel.context.type.invalid:" + channel.name());
        }

        return (SseChannelPlugin<Object>) plugin;
    }

    private String streamId(
        SseChannel channel,
        String resourceId
    ) {
        return channel.getCode() + ":" + resourceId;
    }

    private record SessionState(
        SseChannel channel,
        String resourceId,
        SseEmitter emitter,
        ScheduledFuture<?> heartbeatFuture
    ) {
    }
}
