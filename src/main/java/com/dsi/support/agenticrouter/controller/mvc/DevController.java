package com.dsi.support.agenticrouter.controller.mvc;

import com.dsi.support.agenticrouter.service.PasswordHashService;
import com.dsi.support.agenticrouter.service.VectorStoreInitializationService;
import com.dsi.support.agenticrouter.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dev")
public class DevController {

    private static final Logger logger = LoggerFactory.getLogger(DevController.class);

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
        logger.info("Getting password hash for password: {}", password);

        if (!adminAccessCode.equals(Constants.ADMIN_URL_ACCESS_CODE)) {
            return ResponseEntity.badRequest().body("Invalid Access Code");
        }

        String passwordHash = passwordHashService.getPasswordHash(
            password
        );

        return ResponseEntity.ok("Password Hash Generated Successfully: " + passwordHash);
    }

    @PostMapping("/vector-store/reinitialize")
    public ResponseEntity<?> reinitializeVectorStore(
        @RequestParam("code") String adminAccessCode
    ) {
        logger.info("Vector store re-initialization requested");

        if (!adminAccessCode.equals(Constants.ADMIN_URL_ACCESS_CODE)) {
            logger.warn("Invalid access code for vector store re-initialization");
            return ResponseEntity.badRequest().body("Invalid Access Code");
        }

        vectorStoreInitializationService.forceReinitialize();

        logger.info("Vector store re-initialization completed");
        return ResponseEntity.ok("Vector store re-initialization completed successfully");
    }
}
