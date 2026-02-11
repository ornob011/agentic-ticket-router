package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.service.auth.PasswordHashService;
import com.dsi.support.agenticrouter.service.knowledge.VectorStoreInitializationService;
import com.dsi.support.agenticrouter.util.Constants;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
            "GeneratePasswordHash({}) HttpRequest(path:{}) Outcome(passwordLength:{})",
            OperationalLogContext.PHASE_START,
            "/dev/generate/password-hash",
            password.length()
        );

        if (!adminAccessCode.equals(Constants.ADMIN_URL_ACCESS_CODE)) {
            log.warn(
                "GeneratePasswordHash({}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                "invalid_admin_access_code"
            );

            return ResponseEntity.badRequest().body("Invalid Access Code");
        }

        String passwordHash = passwordHashService.getPasswordHash(
            password
        );

        log.info(
            "GeneratePasswordHash({}) Outcome(hashLength:{})",
            OperationalLogContext.PHASE_COMPLETE,
            passwordHash.length()
        );

        return ResponseEntity.ok("Password Hash Generated Successfully: " + passwordHash);
    }

    @PostMapping("/vector-store/reinitialize")
    public ResponseEntity<?> reinitializeVectorStore(
        @RequestParam("code") String adminAccessCode
    ) {
        log.info(
            "VectorStoreReinitialize({}) HttpRequest(path:{})",
            OperationalLogContext.PHASE_START,
            "/dev/vector-store/reinitialize"
        );

        if (!adminAccessCode.equals(Constants.ADMIN_URL_ACCESS_CODE)) {
            log.warn(
                "VectorStoreReinitialize({}) Outcome(reason:{})",
                OperationalLogContext.PHASE_FAIL,
                "invalid_admin_access_code"
            );

            return ResponseEntity.badRequest().body("Invalid Access Code");
        }

        vectorStoreInitializationService.forceReinitialize();

        log.info(
            "VectorStoreReinitialize({}) Outcome(status:{})",
            OperationalLogContext.PHASE_COMPLETE,
            "success"
        );

        return ResponseEntity.ok("Vector store re-initialization completed successfully");
    }
}
