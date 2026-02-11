package com.dsi.support.agenticrouter.controller.mvc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage() {
        log.debug(
            "LoginPageView(start) Outcome(view:{})",
            "login"
        );

        return "login";
    }
}
