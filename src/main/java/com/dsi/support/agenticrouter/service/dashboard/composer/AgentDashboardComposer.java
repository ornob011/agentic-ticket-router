package com.dsi.support.agenticrouter.service.dashboard.composer;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.service.dashboard.RoleDashboardComposer;
import com.dsi.support.agenticrouter.service.dashboard.section.AgentDashboardSectionAssembler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
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

        log.debug(
            "DashboardComposeAgent(start) Actor(id:{},role:{})",
            dashboardOwnerId,
            dashboardOwner.getRole()
        );

        DashboardDto dashboardDto = DashboardDto.builder()
                                                .user(dashboardOwner)
                                                .agentData(agentSectionAssembler.buildAgentSectionFor(dashboardOwnerId))
                                                .build();
        log.debug(
            "DashboardComposeAgent(complete) Actor(id:{})",
            dashboardOwnerId
        );
        return dashboardDto;
    }
}
