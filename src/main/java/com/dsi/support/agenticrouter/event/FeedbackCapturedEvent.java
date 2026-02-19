package com.dsi.support.agenticrouter.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FeedbackCapturedEvent extends ApplicationEvent {

    private final Long feedbackId;

    public FeedbackCapturedEvent(
        Long feedbackId
    ) {
        super(
            feedbackId
        );
        this.feedbackId = feedbackId;
    }
}
