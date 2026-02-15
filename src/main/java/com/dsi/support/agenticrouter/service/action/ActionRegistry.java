package com.dsi.support.agenticrouter.service.action;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ActionRegistry {

    private final Map<NextAction, TicketAction> actionHandlers;

    public ActionRegistry(
        List<TicketAction> ticketActions
    ) {
        this.actionHandlers = buildActionHandlers(
            ticketActions
        );
    }

    @Transactional
    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        log.info(
            "ActionExecute({}) SupportTicket(id:{},status:{}) RouterResponse(nextAction:{})",
            OperationalLogContext.PHASE_START,
            OperationalLogContext.ticketId(supportTicket),
            OperationalLogContext.status(supportTicket),
            routerResponse.getNextAction()
        );

        TicketAction ticketAction = actionHandlers.get(
            routerResponse.getNextAction()
        );

        if (ticketAction == null) {
            throw new IllegalStateException("No handler for action: " + routerResponse.getNextAction());
        }

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

    private Map<NextAction, TicketAction> buildActionHandlers(
        List<TicketAction> ticketActions
    ) {
        Map<NextAction, TicketAction> handlers = new EnumMap<>(NextAction.class);

        for (NextAction nextAction : NextAction.values()) {
            List<TicketAction> matchingHandlers = ticketActions.stream()
                                                               .filter(handler -> handler.canHandle(nextAction))
                                                               .toList();

            if (matchingHandlers.isEmpty()) {
                continue;
            }

            if (matchingHandlers.size() > 1) {
                String conflictingHandlers = matchingHandlers.stream()
                                                             .map(handler -> handler.getClass().getName())
                                                             .sorted()
                                                             .collect(Collectors.joining(", "));
                throw new IllegalStateException(
                    "Multiple handlers configured for action "
                    + nextAction
                    + ": "
                    + conflictingHandlers
                );
            }

            handlers.put(
                nextAction,
                matchingHandlers.get(0)
            );
        }

        return handlers;
    }
}
