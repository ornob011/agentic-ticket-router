package com.dsi.support.agenticrouter.service.dashboard.composer;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.service.dashboard.RoleDashboardComposer;
import com.dsi.support.agenticrouter.service.dashboard.section.AgentDashboardSectionAssembler;
import org.springframework.stereotype.Component;

@Component
public class AgentDashboardComposer implements RoleDashboardComposer {

    private final AgentDashboardSectionAssembler agentSectionAssembler;

    public AgentDashboardComposer(
        AgentDashboardSectionAssembler agentSectionAssembler
    ) {
        this.agentSectionAssembler = agentSectionAssembler;
    }

    @Override
    public UserRole supportedRole() {
        return UserRole.AGENT;
    }

    @Override
    public DashboardDto composeDashboardViewFor(
        AppUser dashboardOwner
    ) {
        Long dashboardOwnerId = dashboardOwner.getId();

        return DashboardDto.builder()
                           .user(dashboardOwner)
                           .agentData(agentSectionAssembler.buildAgentSectionFor(dashboardOwnerId))
                           .build();
    }
}
