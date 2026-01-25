package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.dto.DashboardDto;
import com.dsi.support.agenticrouter.enums.NavPage;
import com.dsi.support.agenticrouter.service.dashboard.DashboardPageQueryService;
import com.dsi.support.agenticrouter.util.Utils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
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

        DashboardDto dashboardView = dashboardPageQueryService.loadDashboardViewForUser(
            dashboardOwnerId
        );

        model.addAttribute("dashboard", dashboardView);
        model.addAttribute("currentPage", NavPage.DASHBOARD);

        return Utils.getViewWithPrefix(
            "dashboard"
        );
    }
}
