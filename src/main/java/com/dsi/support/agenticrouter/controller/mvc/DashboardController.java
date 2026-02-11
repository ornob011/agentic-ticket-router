package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.enums.NavPage;
import com.dsi.support.agenticrouter.service.dashboard.DashboardPageQueryService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class DashboardController {

    private final DashboardPageQueryService dashboardPageQueryService;

    public DashboardController(
        DashboardPageQueryService dashboardPageQueryService
    ) {
        this.dashboardPageQueryService = dashboardPageQueryService;
    }

    @GetMapping("/dashboard")
    public String renderDashboardPage(Model model) {
        Long dashboardOwnerId = Utils.getLoggedInUserId();

        log.info(
            "DashboardRender({}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            dashboardOwnerId
        );

        DashboardDto dashboardView = dashboardPageQueryService.loadDashboardViewForUser(
            dashboardOwnerId
        );

        model.addAttribute("dashboard", dashboardView);
        model.addAttribute("currentPage", NavPage.DASHBOARD);

        log.info(
            "DashboardRender({}) Actor(id:{}) Outcome(viewLoaded:{})",
            OperationalLogContext.PHASE_COMPLETE,
            dashboardOwnerId,
            dashboardView != null
        );

        return Utils.getViewWithPrefix(
            "dashboard"
        );
    }
}
