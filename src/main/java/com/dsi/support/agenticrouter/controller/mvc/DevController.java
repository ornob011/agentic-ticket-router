package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.service.PasswordHashService;
import com.dsi.support.agenticrouter.service.VectorStoreInitializationService;
import com.dsi.support.agenticrouter.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dev")
@Slf4j
public class DevController {

    private final PasswordHashService passwordHashService;
    private final VectorStoreInitializationService vectorStoreInitializationService;

    public DevController(
        PasswordHashService passwordHashService,
        VectorStoreInitializationService vectorStoreInitializationService
    ) {
        this.passwordHashService = passwordHashService;
        this.vectorStoreInitializationService = vectorStoreInitializationService;
    }

    @GetMapping("/generate/password-hash")
    public ResponseEntity<?> generatePasswordHash(
        @RequestParam("code") String adminAccessCode,
        @RequestParam String password
    ) {
        log.info(
            "GeneratePasswordHash(start) HttpRequest(path:{}) Outcome(passwordLength:{})",
            "/dev/generate/password-hash",
            password.length()
        );

        if (!adminAccessCode.equals(Constants.ADMIN_URL_ACCESS_CODE)) {
            log.warn(
                "GeneratePasswordHash(fail) Outcome(reason:{})",
                "invalid_admin_access_code"
            );

            return ResponseEntity.badRequest().body("Invalid Access Code");
        }

        String passwordHash = passwordHashService.getPasswordHash(
            password
        );

        log.info(
            "GeneratePasswordHash(complete) Outcome(hashLength:{})",
            passwordHash.length()
        );

        return ResponseEntity.ok("Password Hash Generated Successfully: " + passwordHash);
    }

    @PostMapping("/vector-store/reinitialize")
    public ResponseEntity<?> reinitializeVectorStore(
        @RequestParam("code") String adminAccessCode
    ) {
        log.info(
            "VectorStoreReinitialize(start) HttpRequest(path:{})",
            "/dev/vector-store/reinitialize"
        );

        if (!adminAccessCode.equals(Constants.ADMIN_URL_ACCESS_CODE)) {
            log.warn(
                "VectorStoreReinitialize(fail) Outcome(reason:{})",
                "invalid_admin_access_code"
            );

            return ResponseEntity.badRequest().body("Invalid Access Code");
        }

        vectorStoreInitializationService.forceReinitialize();

        log.info(
            "VectorStoreReinitialize(complete) Outcome(status:{})",
            "success"
        );

        return ResponseEntity.ok("Vector store re-initialization completed successfully");
    }
}
