package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage() {
        log.debug(
            "LoginPageView({}) Outcome(view:{})",
            OperationalLogContext.PHASE_START,
            "login"
        );

        return "login";
    }
}
