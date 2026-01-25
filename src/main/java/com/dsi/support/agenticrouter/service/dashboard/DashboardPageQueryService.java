package com.dsi.support.agenticrouter.service.dashboard;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardPageQueryService {

    private final AppUserRepository appUserRepository;
    private final DashboardComposerCatalog dashboardComposerCatalog;

    public DashboardPageQueryService(
        AppUserRepository appUserRepository,
        DashboardComposerCatalog dashboardComposerCatalog
    ) {
        this.appUserRepository = appUserRepository;
        this.dashboardComposerCatalog = dashboardComposerCatalog;
    }

    public DashboardDto loadDashboardViewForUser(Long dashboardOwnerId) {
        AppUser dashboardOwner = appUserRepository.findById(dashboardOwnerId)
                                                  .orElseThrow(
                                                      DataNotFoundException.supplier(
                                                          AppUser.class,
                                                          dashboardOwnerId
                                                      )
                                                  );

        RoleDashboardComposer roleDashboardComposer = dashboardComposerCatalog.composerSupporting(
            dashboardOwner.getRole()
        );

        return roleDashboardComposer.composeDashboardViewFor(
            dashboardOwner
        );
    }
}
