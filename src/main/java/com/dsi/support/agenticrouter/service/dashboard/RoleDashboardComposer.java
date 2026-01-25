package com.dsi.support.agenticrouter.service.dashboard;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.UserRole;

public interface RoleDashboardComposer {

    UserRole supportedRole();

    DashboardDto composeDashboardViewFor(
        AppUser dashboardOwner
    );
}
