package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyConfigService {

    private final PolicyConfigRepository policyConfigRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<PolicyConfig> getAllActivePolicies() {
        return policyConfigRepository.findAllByActiveTrueOrderByConfigKey();
    }

    @Transactional(readOnly = true)
    public PolicyConfig getConfigValue(
        PolicyConfigKey policyConfigKey
    ) {
        return policyConfigRepository.findByConfigKeyAndActiveTrue(policyConfigKey)
                                     .orElse(null);
    }

    @Transactional
    public void updatePolicy(
        PolicyConfigKey policyConfigKey,
        BigDecimal policyConfigValue
    ) {
        Objects.requireNonNull(policyConfigKey, "configKey");
        Objects.requireNonNull(policyConfigValue, "configValue");

        PolicyConfig policyConfig = policyConfigRepository.findByConfigKeyAndActiveTrue(policyConfigKey)
                                                          .orElseThrow(
                                                              DataNotFoundException.supplier(
                                                                  PolicyConfig.class,
                                                                  policyConfigKey
                                                              )
                                                          );

        policyConfig.setConfigValue(policyConfigValue);
        policyConfigRepository.save(policyConfig);
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

    @Transactional
    public void createStaffUser(
        String username,
        String email,
        String fullName,
        UserRole role,
        String passwordHash
    ) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(passwordHash, "passwordHash");

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        if (!role.canAccessAgentPortal()) {
            throw new IllegalArgumentException("Role must be staff role (AGENT, SUPERVISOR, ADMIN)");
        }

        AppUser user = AppUser.builder()
                              .username(username.toLowerCase())
                              .email(email.toLowerCase())
                              .fullName(fullName)
                              .passwordHash(passwordHash)
                              .role(role)
                              .active(true)
                              .emailVerified(true)
                              .build();

        appUserRepository.save(user);

        log.info("Staff user created: {} ({})", username, role);
    }

}
