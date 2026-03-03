package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.service.dashboard.DashboardResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardResponseService dashboardResponseService;

    @GetMapping
    public ApiDtos.DashboardResponse getDashboard() {
        return dashboardResponseService.getDashboardResponse();
    }
}
