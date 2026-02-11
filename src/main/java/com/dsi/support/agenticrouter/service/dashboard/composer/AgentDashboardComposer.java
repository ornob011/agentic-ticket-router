package com.dsi.support.agenticrouter.service.dashboard.composer;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.service.dashboard.RoleDashboardComposer;
import com.dsi.support.agenticrouter.service.dashboard.section.AgentDashboardSectionAssembler;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
            "DashboardComposeAgent({}) Actor(id:{},role:{})",
            OperationalLogContext.PHASE_START,
            dashboardOwnerId,
            dashboardOwner.getRole()
        );

        DashboardDto dashboardDto = DashboardDto.builder()
                                                .user(dashboardOwner)
                                                .agentData(agentSectionAssembler.buildAgentSectionFor(dashboardOwnerId))
                                                .build();
        log.debug(
            "DashboardComposeAgent({}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            dashboardOwnerId
        );
        return dashboardDto;
    }
}
