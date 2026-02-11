package com.dsi.support.agenticrouter.service.action;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
        log.info(
            "ActionExecute({}) SupportTicket(id:{},status:{}) RouterResponse(nextAction:{})",
            OperationalLogContext.PHASE_START,
            OperationalLogContext.ticketId(supportTicket),
            OperationalLogContext.status(supportTicket),
            routerResponse.getNextAction()
        );

        TicketAction ticketAction = ticketActions.stream()
                                                 .filter(handler -> handler.canHandle(routerResponse.getNextAction()))
                                                 .findFirst()
                                                 .orElseThrow(
                                                     () -> new IllegalStateException("No handler for action: " + routerResponse.getNextAction())
                                                 );

        log.debug(
            "ActionExecute({}) SupportTicket(id:{}) Outcome(handler:{})",
            OperationalLogContext.PHASE_DECISION,
            OperationalLogContext.ticketId(supportTicket),
            ticketAction.getClass().getSimpleName()
        );

        ticketAction.execute(
            supportTicket,
            routerResponse
        );

        log.info(
            "ActionExecute({}) SupportTicket(id:{},status:{}) Outcome(nextAction:{})",
            OperationalLogContext.PHASE_COMPLETE,
            OperationalLogContext.ticketId(supportTicket),
            OperationalLogContext.status(supportTicket),
            routerResponse.getNextAction()
        );
    }
}
