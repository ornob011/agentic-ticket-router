package com.dsi.support.agenticrouter.service.dashboard.composer;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.service.dashboard.RoleDashboardComposer;
import com.dsi.support.agenticrouter.service.dashboard.section.AgentDashboardSectionAssembler;
import com.dsi.support.agenticrouter.service.dashboard.section.SupervisorDashboardSectionAssembler;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SupervisorDashboardComposer implements RoleDashboardComposer {

    private final AgentDashboardSectionAssembler agentSectionAssembler;
    private final SupervisorDashboardSectionAssembler supervisorSectionAssembler;

    public SupervisorDashboardComposer(
        AgentDashboardSectionAssembler agentSectionAssembler,
        SupervisorDashboardSectionAssembler supervisorSectionAssembler
    ) {
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

        log.debug(
            "DashboardComposeSupervisor({}) Actor(id:{},role:{})",
            OperationalLogContext.PHASE_START,
            dashboardOwnerId,
            dashboardOwner.getRole()
        );

        DashboardDto dashboardDto = DashboardDto.builder()
                                                .user(dashboardOwner)
                                                .agentData(agentSectionAssembler.buildAgentSectionFor(dashboardOwnerId))
                                                .supervisorData(supervisorSectionAssembler.buildSupervisorSection())
                                                .build();
        log.debug(
            "DashboardComposeSupervisor({}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            dashboardOwnerId
        );
        return dashboardDto;
    }
}
