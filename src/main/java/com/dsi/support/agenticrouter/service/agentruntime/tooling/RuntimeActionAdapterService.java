package com.dsi.support.agenticrouter.service.agentruntime.tooling;

import com.dsi.support.agenticrouter.dto.RouterResponse;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.service.action.ActionRegistry;
import com.dsi.support.agenticrouter.service.routing.RouterResponseContractValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;

@Service
@RequiredArgsConstructor
public class RuntimeActionAdapterService {

    private final RouterResponseContractValidator routerResponseContractValidator;
    private final ActionRegistry actionRegistry;

    public void execute(
        SupportTicket supportTicket,
        RouterResponse routerResponse
    ) throws BindException {
        routerResponseContractValidator.validate(
            routerResponse
        );

        actionRegistry.execute(
            supportTicket,
            routerResponse
        );
    }
}
