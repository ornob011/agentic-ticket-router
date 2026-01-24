package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.service.PasswordHashService;
import com.dsi.support.agenticrouter.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dev")
public class DevController {

    private static final Logger logger = LoggerFactory.getLogger(DevController.class);

    private final PasswordHashService passwordHashService;

    public DevController(PasswordHashService passwordHashService) {
        this.passwordHashService = passwordHashService;
    }

    @GetMapping("/generate/password-hash")
    public ResponseEntity<?> generatePasswordHash(
        @RequestParam("code") String adminAccessCode,
        @RequestParam String password
    ) {
        logger.info("Getting password hash for password: {}", password);

        if (!adminAccessCode.equals(Constants.ADMIN_URL_ACCESS_CODE)) {
            return ResponseEntity.badRequest().body("Invalid Access Code");
        }

        String passwordHash = passwordHashService.getPasswordHash(
            password
        );

        return ResponseEntity.ok("Password Hash Generated Successfully: " + passwordHash);
    }
}
