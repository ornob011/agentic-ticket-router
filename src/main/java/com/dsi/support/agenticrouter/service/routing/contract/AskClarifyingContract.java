package com.dsi.support.agenticrouter.service.routing.contract;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class AskClarifyingContract implements RouterNextActionContract {

    @Override
    public NextAction action() {
        return NextAction.ASK_CLARIFYING;
    }

    @Override
    public void validate(
        RouterResponse routerResponse
    ) {
        if (StringUtils.isBlank(routerResponse.getClarifyingQuestion())) {
            throw new IllegalStateException("clarifying_question is required for ASK_CLARIFYING");
        }
    }
}
