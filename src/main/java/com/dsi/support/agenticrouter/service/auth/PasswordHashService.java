package com.dsi.support.agenticrouter.service.auth;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
            "PasswordHashGenerate({}) Outcome(rawLength:{})",
            OperationalLogContext.PHASE_START,
            StringUtils.length(rawValue)
        );

        if (StringUtils.isBlank(rawValue)) {
            throw new IllegalArgumentException("Password value must not be null or empty");
        }

        String passwordHash = passwordEncoder.encode(
            rawValue
        );

        log.debug(
            "PasswordHashGenerate({}) Outcome(hashLength:{})",
            OperationalLogContext.PHASE_COMPLETE,
            StringUtils.length(passwordHash)
        );

        return passwordHash;
    }

    public boolean passwordHashMatches(
        String rawValue,
        String hashValue
    ) {
        log.debug(
            "PasswordHashVerify({}) Outcome(rawLength:{},hashLength:{})",
            OperationalLogContext.PHASE_START,
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
            "PasswordHashVerify({}) Outcome(matches:{})",
            OperationalLogContext.PHASE_COMPLETE,
            matches
        );

        return matches;
    }
}
