package com.dsi.support.agenticrouter.service.agentruntime.rollout;

import com.dsi.support.agenticrouter.configuration.AgentRuntimeConfiguration;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentRuntimeEligibilityService {

    private final AgentRuntimeConfiguration agentRuntimeConfiguration;

    public boolean isEligible(
        SupportTicket supportTicket
    ) {
        if (!agentRuntimeConfiguration.isEnabled()) {
            return false;
        }

        if (!agentRuntimeConfiguration.isCanaryEnabled()) {
            return true;
        }

        TicketQueue ticketQueue = supportTicket.getAssignedQueue();

        return ticketQueue != null
               && agentRuntimeConfiguration.getAllowedQueues().contains(ticketQueue);
    }
}
