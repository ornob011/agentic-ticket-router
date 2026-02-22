package com.dsi.support.agenticrouter.service.agentruntime.streaming;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.service.sse.SseChannel;
import com.dsi.support.agenticrouter.service.sse.SseChannelPlugin;
import com.dsi.support.agenticrouter.service.sse.SseEventType;
import com.dsi.support.agenticrouter.service.sse.SseSession;
import com.dsi.support.agenticrouter.service.ticket.TicketCommandLookupService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
@Slf4j
public class DraftReplySsePlugin implements SseChannelPlugin<Long> {

    private static final long EMITTER_TIMEOUT_MS = 60_000L;

    private final TicketCommandLookupService ticketCommandLookupService;
    private final StreamingDraftService streamingDraftService;

    @Override
    public SseChannel channel() {
        return SseChannel.DRAFT_REPLY;
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
        return EMITTER_TIMEOUT_MS;
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
        SupportTicket ticket = ticketCommandLookupService.requireTicket(context);

        log.debug(
            "DraftReplySsePlugin({}) SupportTicket(id:{}) Outcome(subscribe)",
            OperationalLogContext.PHASE_START,
            context
        );

        Schedulers.boundedElastic().schedule(() ->
            streamingDraftService.streamDraftReply(ticket)
                                 .doOnNext(token ->
                                     session.publish(
                                         SseEventType.TOKEN,
                                         new DraftTokenPayload(token)
                                     )
                                 )
                                 .doOnComplete(() -> {
                                     session.complete(
                                         new DraftDonePayload(
                                             SseEventType.DONE.name()
                                         )
                                     );
                                     log.debug(
                                         "DraftReplySsePlugin({}) SupportTicket(id:{}) Outcome(completed)",
                                         OperationalLogContext.PHASE_COMPLETE,
                                         context
                                     );
                                 })
                                 .doOnError(error -> {
                                     log.error(
                                         "DraftReplySsePlugin({}) SupportTicket(id:{}) Outcome(reason:{})",
                                         OperationalLogContext.PHASE_FAIL,
                                         context,
                                         "stream_failed",
                                         error
                                     );
                                     session.error(
                                         new DraftDonePayload(
                                             SseEventType.ERROR.name()
                                         )
                                     );
                                 })
                                 .subscribe()
        );
    }
}
