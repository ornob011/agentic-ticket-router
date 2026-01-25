package com.dsi.support.agenticrouter.service.dashboard.composer;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.service.dashboard.RoleDashboardComposer;
import com.dsi.support.agenticrouter.service.dashboard.section.AgentDashboardSectionAssembler;
import com.dsi.support.agenticrouter.service.dashboard.section.SupervisorDashboardSectionAssembler;
import org.springframework.stereotype.Component;

@Component
public class SupervisorDashboardComposer implements RoleDashboardComposer {

    private final AgentDashboardSectionAssembler agentSectionAssembler;
    private final SupervisorDashboardSectionAssembler supervisorSectionAssembler;

    public SupervisorDashboardComposer(AgentDashboardSectionAssembler agentSectionAssembler,
                                       SupervisorDashboardSectionAssembler supervisorSectionAssembler) {
        this.agentSectionAssembler = agentSectionAssembler;
        this.supervisorSectionAssembler = supervisorSectionAssembler;
    }

    @Override
    public UserRole supportedRole() {
        return UserRole.SUPERVISOR;
    }

    @Override
    public DashboardDto composeDashboardViewFor(AppUser dashboardOwner) {
        Long dashboardOwnerId = dashboardOwner.getId();

        return DashboardDto.builder()
                           .user(dashboardOwner)
                           .agentData(agentSectionAssembler.buildAgentSectionFor(dashboardOwnerId))
                           .supervisorData(supervisorSectionAssembler.buildSupervisorSection())
                           .build();
    }
}
