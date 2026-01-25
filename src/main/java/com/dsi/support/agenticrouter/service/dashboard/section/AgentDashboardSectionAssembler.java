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

        List<SupportTicketRepository.TicketQueueCount> groupedQueueCounts;
        groupedQueueCounts = supportTicketRepository.countTicketsGroupedByAssignedQueue();

        Map<TicketQueue, Long> groupedCountsByQueue = new EnumMap<>(TicketQueue.class);

        for (SupportTicketRepository.TicketQueueCount queueCount : groupedQueueCounts) {
            groupedCountsByQueue.put(
                queueCount.getQueue(),
                queueCount.getCount()
            );
        }

        for (TicketQueue queue : TicketQueue.values()) {
            ticketsInEachQueue.put(
                queue,
                groupedCountsByQueue.getOrDefault(
                    queue,
                    0L
                )
            );
        }

        SupportTicketRepository.AgentTicketCounts agentCounts;
        agentCounts = supportTicketRepository.countAgentTicketCounts(
            agentId
        );

        long ticketsAssignedToAgent = agentCounts.getTotalCount();

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
            agentCounts.getInProgressCount()
        );

        agentTicketCountsByStatus.put(
            TicketStatus.WAITING_CUSTOMER,
            agentCounts.getWaitingCustomerCount()
        );

        return DashboardDto.AgentData.builder()
                                     .queueCounts(ticketsInEachQueue)
                                     .myAssignedCount(ticketsAssignedToAgent)
                                     .myTickets(mostRecentlyActiveAssignedTickets)
                                     .myTicketStatusCounts(agentTicketCountsByStatus)
                                     .build();
    }
}
