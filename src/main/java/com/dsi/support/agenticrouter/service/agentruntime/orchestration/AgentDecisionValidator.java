package com.dsi.support.agenticrouter.service.agentruntime.orchestration;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.service.routing.RouterResponseContractValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AgentDecisionValidator {

    private final RouterResponseContractValidator routerResponseContractValidator;

    public void validate(
        RouterResponse routerResponse
    ) {
        Objects.requireNonNull(routerResponse, "routerResponse");

        routerResponseContractValidator.validate(
            routerResponse
        );
    }
}
