package com.dsi.support.agenticrouter.service.action;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NextAction;
import org.springframework.validation.BindException;

public interface TicketAction {
    void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException;

    boolean canHandle(
        NextAction actionType
    );
}
