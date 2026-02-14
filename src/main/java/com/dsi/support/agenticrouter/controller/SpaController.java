package com.dsi.support.agenticrouter.controller;

import com.dsi.support.agenticrouter.configuration.AppRoutePolicy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
        AppRoutePolicy.PATH_APP,
        AppRoutePolicy.PATH_APP_SLASH,
        AppRoutePolicy.PATH_LOGIN,
        AppRoutePolicy.PATH_SIGNUP,
        AppRoutePolicy.PATH_DASHBOARD,
        AppRoutePolicy.PATTERN_TICKETS_SCOPE,
        AppRoutePolicy.PATTERN_AGENT_SCOPE,
        AppRoutePolicy.PATTERN_SUPERVISOR_SCOPE,
        AppRoutePolicy.PATTERN_ADMIN_SCOPE
    })
    public String app() {
        return AppRoutePolicy.APP_INDEX_FORWARD;
    }
}
