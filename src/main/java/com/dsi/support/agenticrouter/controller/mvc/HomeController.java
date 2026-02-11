package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@Slf4j
public class HomeController {

    @GetMapping("/")
    public String home(
        UriComponentsBuilder uriComponentsBuilder
    ) {
        log.debug(
            "HomeRoute(start) Outcome(isLoggedIn:{})",
            Utils.isLoggedIn()
        );

        if (!Utils.isLoggedIn()) {
            log.info(
                "HomeRoute(decision) Outcome(action:{})",
                "redirect_login"
            );

            return "redirect:/login";
        }

        String redirectUrl = uriComponentsBuilder.path("/dashboard")
                                                 .toUriString();

        log.info(
            "HomeRoute(complete) Outcome(action:{},target:{})",
            "redirect_dashboard",
            redirectUrl
        );

        return "redirect:" + redirectUrl;
    }
}
