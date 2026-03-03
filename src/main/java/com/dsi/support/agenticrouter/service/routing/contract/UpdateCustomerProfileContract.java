package com.dsi.support.agenticrouter.service.routing.contract;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.RoutingActionParameterKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UpdateCustomerProfileContract implements RouterNextActionContract {

    @Override
    public NextAction action() {
        return NextAction.UPDATE_CUSTOMER_PROFILE;
    }

    @Override
    public void validate(
        RouterResponse routerResponse
    ) {
        Map<String, ?> actionParameters = routerResponse.getActionParameters();
        if (actionParameters == null) {
            throw new IllegalStateException("action_parameters is required for UPDATE_CUSTOMER_PROFILE");
        }

        boolean hasAnyProfileField = RoutingActionParameterKey.profileUpdateFields()
                                                              .stream()
                                                              .map(field -> actionParameters.get(field.getKey()))
                                                              .map(value -> value == null ? null : value.toString().trim())
                                                              .anyMatch(StringUtils::isNotBlank);
        if (!hasAnyProfileField) {
            throw new IllegalStateException("at least one profile field is required for UPDATE_CUSTOMER_PROFILE");
        }
    }
}
