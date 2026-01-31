package com.dsi.support.agenticrouter.service.dashboard.section;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.ModelService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AdminDashboardSectionAssembler {

    private final AppUserRepository appUserRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ModelService modelService;

    public AdminDashboardSectionAssembler(
        AppUserRepository appUserRepository,
        SupportTicketRepository supportTicketRepository,
        ModelService modelService
    ) {
        this.appUserRepository = appUserRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.modelService = modelService;
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

        String activeModelTag = modelService.getActiveModelTag();

        return new DashboardDto.AdminData(
            totalRegisteredUsers,
            totalTicketsInSystem,
            activeModelTag,
            routingSuccessRate
        );
    }
}
