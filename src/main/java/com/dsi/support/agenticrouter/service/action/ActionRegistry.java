package com.dsi.support.agenticrouter.service.action;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionRegistry {

    private final List<TicketAction> ticketActions;

    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) {
        TicketAction ticketAction = ticketActions.stream()
                                                 .filter(handler -> handler.canHandle(routerResponse.getNextAction()))
                                                 .findFirst()
                                                 .orElseThrow(
                                                     () -> new IllegalStateException("No handler for action: " + routerResponse.getNextAction())
                                                 );

        ticketAction.execute(
            supportTicket,
            routerResponse
        );
    }
}
