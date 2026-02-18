package com.dsi.support.agenticrouter.service.routing.contract;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.RoutingActionParameterKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UseTemplateContract implements RouterNextActionContract {

    private final RoutingActionParameterReader routingActionParameterReader;

    @Override
    public NextAction action() {
        return NextAction.USE_TEMPLATE;
    }

    @Override
    public void validate(
        RouterResponse routerResponse
    ) {
        routingActionParameterReader.requirePositiveLong(
            routerResponse.getActionParameters(),
            RoutingActionParameterKey.TEMPLATE_ID,
            "template_id is required for USE_TEMPLATE",
            "template_id must be numeric for USE_TEMPLATE",
            "template_id must be positive for USE_TEMPLATE"
        );
    }
}
