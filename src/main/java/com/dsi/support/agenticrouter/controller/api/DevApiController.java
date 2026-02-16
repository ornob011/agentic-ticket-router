package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.service.auth.PasswordHashService;
import com.dsi.support.agenticrouter.service.knowledge.VectorStoreInitializationService;
import com.dsi.support.agenticrouter.util.Constants;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev")
@Slf4j
public class DevApiController {

    private final PasswordHashService passwordHashService;
    private final VectorStoreInitializationService vectorStoreInitializationService;

    public DevApiController(
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
            "/api/dev/generate/password-hash",
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
            "/api/dev/vector-store/reinitialize"
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
