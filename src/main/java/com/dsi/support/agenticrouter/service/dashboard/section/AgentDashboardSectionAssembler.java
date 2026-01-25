package com.dsi.support.agenticrouter.service.dashboard.section;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentDashboardSectionAssembler {

    private final SupportTicketRepository supportTicketRepository;

    public AgentDashboardSectionAssembler(
        SupportTicketRepository supportTicketRepository
    ) {
        this.supportTicketRepository = supportTicketRepository;
    }

    public DashboardDto.AgentData buildAgentSectionFor(
        Long agentId
    ) {
        Map<TicketQueue, Long> ticketsInEachQueue = new EnumMap<>(TicketQueue.class);

        for (TicketQueue queue : TicketQueue.values()) {
            long ticketsCurrentlyInQueue = supportTicketRepository.countByAssignedQueue(
                queue
            );

            ticketsInEachQueue.put(
                queue,
                ticketsCurrentlyInQueue
            );
        }

        long ticketsAssignedToAgent = supportTicketRepository.countByAssignedAgentId(
            agentId
        );

        List<DashboardDto.TicketSummary> mostRecentlyActiveAssignedTickets;
        mostRecentlyActiveAssignedTickets = supportTicketRepository.findTop5ByAssignedAgentIdOrderByLastActivityAtDesc(
                                                                       agentId
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

        Map<TicketStatus, Long> agentTicketCountsByStatus = new EnumMap<>(TicketStatus.class);

        agentTicketCountsByStatus.put(
            TicketStatus.IN_PROGRESS,
            supportTicketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.IN_PROGRESS)
        );

        agentTicketCountsByStatus.put(
            TicketStatus.WAITING_CUSTOMER,
            supportTicketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.WAITING_CUSTOMER)
        );

        return DashboardDto.AgentData.builder()
                                     .queueCounts(ticketsInEachQueue)
                                     .myAssignedCount(ticketsAssignedToAgent)
                                     .myTickets(mostRecentlyActiveAssignedTickets)
                                     .myTicketStatusCounts(agentTicketCountsByStatus)
                                     .build();
    }
}
