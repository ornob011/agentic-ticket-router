package com.dsi.support.agenticrouter.service.dashboard.section;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AdminDashboardSectionAssembler {

    private final AppUserRepository appUserRepository;
    private final SupportTicketRepository supportTicketRepository;

    public AdminDashboardSectionAssembler(
        AppUserRepository appUserRepository,
        SupportTicketRepository supportTicketRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.supportTicketRepository = supportTicketRepository;
    }

    public DashboardDto.AdminData buildAdminSection() {
        long totalRegisteredUsers = appUserRepository.count();
        long totalTicketsInSystem = supportTicketRepository.count();

        long highConfidenceRoutingCount = supportTicketRepository.countHighConfidenceRoutings(
            new BigDecimal("0.85")
        );

        double routingSuccessRate = 0.0;

        if (totalTicketsInSystem > 0) {
            routingSuccessRate = (double) highConfidenceRoutingCount / (double) totalTicketsInSystem;
        }

        return DashboardDto.AdminData.builder()
                                     .totalUsers(totalRegisteredUsers)
                                     .totalTickets(totalTicketsInSystem)
                                     .routingSuccessRate(routingSuccessRate)
                                     .build();
    }
}
