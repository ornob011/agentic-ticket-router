package com.dsi.support.agenticrouter.security;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.TicketQueryScope;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TicketAuthorizationService {

    private final SupportTicketRepository supportTicketRepository;

    public boolean canAccessTicket(
        Long ticketId
    ) {
        if (Objects.isNull(ticketId)) {
            return false;
        }

        AppUser loggedInUser = Utils.getLoggedInUserDetails();
        if (!loggedInUser.isCustomer()) {
            return true;
        }

        return supportTicketRepository.existsByIdAndCustomerId(
            ticketId,
            loggedInUser.getId()
        );
    }

    public boolean canAccessQueueScope(
        TicketQueryScope scope
    ) {
        if (Objects.isNull(scope)) {
            return false;
        }

        AppUser loggedInUser = Utils.getLoggedInUserDetails();
        return switch (scope) {
            case MINE -> true;
            case QUEUE, ALL -> loggedInUser.canAccessAgentPortal();
            case REVIEW -> loggedInUser.isSupervisor() || loggedInUser.isAdmin();
        };
    }
}
