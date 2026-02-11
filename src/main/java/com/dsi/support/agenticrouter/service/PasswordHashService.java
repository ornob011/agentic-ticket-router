package com.dsi.support.agenticrouter.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PasswordHashService {
    private final PasswordEncoder passwordEncoder;

    public PasswordHashService(
        PasswordEncoder passwordEncoder
    ) {
        this.passwordEncoder = passwordEncoder;
    }

    public String getPasswordHash(
        String rawValue
    ) {
        log.debug(
            "PasswordHashGenerate(start) Outcome(rawLength:{})",
            StringUtils.length(rawValue)
        );

        if (StringUtils.isBlank(rawValue)) {
            throw new IllegalArgumentException("Password value must not be null or empty");
        }

        String passwordHash = passwordEncoder.encode(
            rawValue
        );

        log.debug(
            "PasswordHashGenerate(complete) Outcome(hashLength:{})",
            StringUtils.length(passwordHash)
        );

        return passwordHash;
    }

    public boolean passwordHashMatches(
        String rawValue,
        String hashValue
    ) {
        log.debug(
            "PasswordHashVerify(start) Outcome(rawLength:{},hashLength:{})",
            StringUtils.length(rawValue),
            StringUtils.length(hashValue)
        );

        if (StringUtils.isBlank(rawValue) || StringUtils.isBlank(hashValue)) {
            return false;
        }

        boolean matches = passwordEncoder.matches(
            rawValue,
            hashValue
        );

        log.debug(
            "PasswordHashVerify(complete) Outcome(matches:{})",
            matches
        );

        return matches;
    }
}
