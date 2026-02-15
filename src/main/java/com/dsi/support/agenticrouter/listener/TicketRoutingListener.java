package com.dsi.support.agenticrouter.listener;

import com.dsi.support.agenticrouter.event.TicketCreatedEvent;
import com.dsi.support.agenticrouter.service.routing.RouterOrchestrator;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.validation.BindException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketRoutingListener {

    private final RouterOrchestrator routerOrchestrator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketCreated(
        TicketCreatedEvent event
    ) throws BindException {
        log.info(
            "TicketCreatedEventHandle({}) SupportTicket(id:{})",
            OperationalLogContext.PHASE_START,
            event.getTicketId()
        );

        routerOrchestrator.routeTicket(
            event.getTicketId()
        );

        log.info(
            "TicketCreatedEventHandle({}) SupportTicket(id:{}) Outcome(dispatch:{})",
            OperationalLogContext.PHASE_COMPLETE,
            event.getTicketId(),
            "async_routing_triggered"
        );
    }

}
