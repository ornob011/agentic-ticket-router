package com.dsi.support.agenticrouter.security;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.TicketQueryScope;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TicketAuthorizationService {

    private final TicketAccessPolicyService ticketAccessPolicyService;

    public boolean canAccessTicket(
        Long ticketId
    ) {
        if (Objects.isNull(ticketId)) {
            return false;
        }

        AppUser loggedInUser = Utils.getLoggedInUserDetails();
        return ticketAccessPolicyService.canAccessTicket(
            ticketId,
            loggedInUser
        );
    }

    public boolean canAccessQueueScope(
        TicketQueryScope scope
    ) {
        return canAccessQueueScope(
            scope,
            null
        );
    }

    public boolean canAccessQueueScope(
        TicketQueryScope scope,
        TicketQueue queue
    ) {
        if (Objects.isNull(scope)) {
            return false;
        }

        AppUser loggedInUser = Utils.getLoggedInUserDetails();
        return ticketAccessPolicyService.canAccessQueueScope(
            scope,
            queue,
            loggedInUser
        );
    }
}
