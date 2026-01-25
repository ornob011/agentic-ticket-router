package com.dsi.support.agenticrouter.service.dashboard.section;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerDashboardSectionAssembler {

    private final SupportTicketRepository supportTicketRepository;

    public CustomerDashboardSectionAssembler(
        SupportTicketRepository supportTicketRepository
    ) {
        this.supportTicketRepository = supportTicketRepository;
    }

    public DashboardDto.CustomerData buildCustomerSectionFor(Long customerId) {
        SupportTicketRepository.CustomerTicketCounts counts;
        counts = supportTicketRepository.countCustomerTicketCounts(
            customerId
        );

        List<DashboardDto.TicketSummary> mostRecentCustomerTickets;
        mostRecentCustomerTickets = supportTicketRepository.findTop5ByCustomerIdOrderByLastActivityAtDesc(
                                                               customerId
                                                           )
                                                           .stream()
                                                           .map(
                                                               supportTicket -> DashboardDto.TicketSummary.builder()
                                                                                                          .id(supportTicket.getId())
                                                                                                          .ticketNo(supportTicket.getTicketNo().toString())
                                                                                                          .formattedTicketNo(supportTicket.getFormattedTicketNo())
                                                                                                          .subject(supportTicket.getSubject())
                                                                                                          .status(supportTicket.getStatus())
                                                                                                          .lastActivityAt(supportTicket.getLastActivityAt())
                                                                                                          .build()
                                                           )
                                                           .toList();

        return DashboardDto.CustomerData.builder()
                                        .openTickets(counts.getOpenCount())
                                        .waitingOnMe(counts.getWaitingCustomerCount())
                                        .resolvedTickets(counts.getResolvedCount())
                                        .closedTickets(counts.getClosedCount())
                                        .recentTickets(mostRecentCustomerTickets)
                                        .build();
    }
}
