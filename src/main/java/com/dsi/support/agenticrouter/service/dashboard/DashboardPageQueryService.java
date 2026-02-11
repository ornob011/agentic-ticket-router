package com.dsi.support.agenticrouter.service.dashboard;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
        log.debug(
            "DashboardCompose({}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            dashboardOwnerId
        );

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

        DashboardDto dashboardDto = roleDashboardComposer.composeDashboardViewFor(
            dashboardOwner
        );

        log.debug(
            "DashboardCompose({}) Actor(id:{},role:{}) Outcome(composer:{})",
            OperationalLogContext.PHASE_COMPLETE,
            dashboardOwner.getId(),
            dashboardOwner.getRole(),
            roleDashboardComposer.getClass().getSimpleName()
        );

        return dashboardDto;
    }
}
