package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketCommandLookupService {

    private final SupportTicketRepository supportTicketRepository;
    private final AppUserRepository appUserRepository;

    public SupportTicket requireTicket(
        Long ticketId
    ) {
        return supportTicketRepository.findById(ticketId)
                                      .orElseThrow(
                                          DataNotFoundException.supplier(
                                              SupportTicket.class,
                                              ticketId
                                          )
                                      );
    }

    public AppUser requireUser(
        Long userId
    ) {
        return appUserRepository.findById(userId)
                                .orElseThrow(
                                    DataNotFoundException.supplier(
                                        AppUser.class,
                                        userId
                                    )
                                );
    }

    public AppUser requireCurrentActor() {
        return requireUser(
            Utils.getLoggedInUserId()
        );
    }
}
