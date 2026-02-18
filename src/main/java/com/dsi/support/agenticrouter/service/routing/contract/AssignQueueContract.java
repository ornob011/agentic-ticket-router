package com.dsi.support.agenticrouter.service.routing.contract;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class AssignQueueContract implements RouterNextActionContract {

    @Override
    public NextAction action() {
        return NextAction.ASSIGN_QUEUE;
    }

    @Override
    public void validate(
        RouterResponse routerResponse
    ) {
        if (StringUtils.isNotBlank(routerResponse.getClarifyingQuestion())) {
            throw new IllegalStateException("clarifying_question must be null for ASSIGN_QUEUE");
        }

        if (StringUtils.isNotBlank(routerResponse.getDraftReply())) {
            throw new IllegalStateException("draft_reply must be null for ASSIGN_QUEUE");
        }
    }
}
