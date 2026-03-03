package com.dsi.support.agenticrouter.service.routing.contract;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;

public interface RouterNextActionContract {

    NextAction action();

    void validate(
        RouterResponse routerResponse
    );
}
