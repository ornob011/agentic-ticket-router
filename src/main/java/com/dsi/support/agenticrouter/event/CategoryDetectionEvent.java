package com.dsi.support.agenticrouter.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CategoryDetectionEvent extends ApplicationEvent {

    private final Long ticketId;
    private final String replyContent;
    private final Long customerId;

    public CategoryDetectionEvent(
        Object source,
        Long ticketId,
        String replyContent,
        Long customerId
    ) {
        super(source);
        this.ticketId = ticketId;
        this.replyContent = replyContent;
        this.customerId = customerId;
    }
}
