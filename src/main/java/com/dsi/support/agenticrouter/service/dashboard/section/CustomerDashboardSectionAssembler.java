package com.dsi.support.agenticrouter.service.dashboard.section;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.enums.TicketStatus;
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
        long received = supportTicketRepository.countByCustomerIdAndStatus(
            customerId,
            TicketStatus.RECEIVED
        );

        long triaging = supportTicketRepository.countByCustomerIdAndStatus(
            customerId,
            TicketStatus.TRIAGING
        );

        long assigned = supportTicketRepository.countByCustomerIdAndStatus(
            customerId,
            TicketStatus.ASSIGNED
        );

        long inProgress = supportTicketRepository.countByCustomerIdAndStatus(
            customerId,
            TicketStatus.IN_PROGRESS
        );

        long currentlyOpenTickets = received + triaging + assigned + inProgress;

        long ticketsWaitingForCustomer = supportTicketRepository.countByCustomerIdAndStatus(
            customerId,
            TicketStatus.WAITING_CUSTOMER
        );

        long ticketsResolved = supportTicketRepository.countByCustomerIdAndStatus(
            customerId,
            TicketStatus.RESOLVED
        );

        long ticketsClosed = supportTicketRepository.countByCustomerIdAndStatus(
            customerId,
            TicketStatus.CLOSED
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
                                        .openTickets(currentlyOpenTickets)
                                        .waitingOnMe(ticketsWaitingForCustomer)
                                        .resolvedTickets(ticketsResolved)
                                        .closedTickets(ticketsClosed)
                                        .recentTickets(mostRecentCustomerTickets)
                                        .build();
    }
}
