package com.dsi.support.agenticrouter.service.dashboard.section;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SupervisorDashboardSectionAssembler {

    private final EscalationRepository escalationRepository;
    private final SupportTicketRepository supportTicketRepository;

    public SupervisorDashboardSectionAssembler(
        EscalationRepository escalationRepository,
        SupportTicketRepository supportTicketRepository
    ) {
        this.escalationRepository = escalationRepository;
        this.supportTicketRepository = supportTicketRepository;
    }

    public DashboardDto.SupervisorData buildSupervisorSection() {
        long pendingEscalations = escalationRepository.countByResolvedFalse();

        Instant slaBreachCutoffInstant = Instant.now()
                                                .minus(
                                                    24,
                                                    ChronoUnit.HOURS
                                                );

        long slaBreaches = supportTicketRepository.countSlaBreaches(
            slaBreachCutoffInstant
        );

        List<DashboardDto.EscalationSummary> mostRecentOpenEscalations;
        mostRecentOpenEscalations = escalationRepository.findTop10ByResolvedFalseOrderByCreatedAtDesc()
                                                        .stream()
                                                        .map(
                                                            escalation -> DashboardDto.EscalationSummary.builder()
                                                                                                        .id(escalation.getId())
                                                                                                        .ticketNo(escalation.getTicket().getFormattedTicketNo())
                                                                                                        .reason(escalation.getReason())
                                                                                                        .createdAt(escalation.getCreatedAt())
                                                                                                        .resolved(escalation.isResolved())
                                                                                                        .build()
                                                        )
                                                        .toList();

        return DashboardDto.SupervisorData.builder()
                                          .pendingEscalations(pendingEscalations)
                                          .slaBreaches(slaBreaches)
                                          .recentEscalations(mostRecentOpenEscalations)
                                          .build();
    }
}
