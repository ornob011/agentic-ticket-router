package com.dsi.support.agenticrouter.listener;

import com.dsi.support.agenticrouter.event.TicketCreatedEvent;
import com.dsi.support.agenticrouter.service.routing.RouterOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketRoutingListener {

    private final RouterOrchestrator routerOrchestrator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketCreated(
        TicketCreatedEvent event
    ) {
        log.info("Handling ticket created event for ticket ID: {}", event.getTicketId());

        routerOrchestrator.routeTicket(
            event.getTicketId()
        );
    }

}
