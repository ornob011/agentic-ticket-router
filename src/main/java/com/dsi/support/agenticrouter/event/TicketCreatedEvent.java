package com.dsi.support.agenticrouter.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TicketCreatedEvent extends ApplicationEvent {

    private final Long ticketId;

    public TicketCreatedEvent(
        Object source,
        Long ticketId
    ) {
        super(source);
        this.ticketId = ticketId;
    }

}
