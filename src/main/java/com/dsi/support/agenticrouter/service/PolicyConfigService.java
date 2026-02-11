package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
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
        List<PolicyConfig> policies = policyConfigRepository.findAllByActiveTrueOrderByConfigKey();

        log.debug(
            "PolicyConfigList({}) Outcome(policyCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            policies.size()
        );

        return policies;
    }

    @Transactional(readOnly = true)
    public PolicyConfig getConfigValue(
        PolicyConfigKey policyConfigKey
    ) {
        PolicyConfig policyConfig = policyConfigRepository.findByConfigKeyAndActiveTrue(policyConfigKey)
                                                          .orElse(null);

        log.debug(
            "PolicyConfigLookup({}) PolicyConfig(key:{},activeFound:{})",
            OperationalLogContext.PHASE_COMPLETE,
            policyConfigKey,
            Objects.nonNull(policyConfig)
        );
        return policyConfig;
    }

    @Transactional
    public void updatePolicy(
        PolicyConfigKey policyConfigKey,
        BigDecimal policyConfigValue
    ) {
        log.info(
            "PolicyConfigUpdate({}) PolicyConfig(key:{}) Outcome(newValue:{})",
            OperationalLogContext.PHASE_START,
            policyConfigKey,
            policyConfigValue
        );

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

        log.info(
            "PolicyConfigUpdate({}) PolicyConfig(key:{}) Outcome(updatedValue:{})",
            OperationalLogContext.PHASE_COMPLETE,
            policyConfig.getConfigKey(),
            policyConfig.getConfigValue()
        );
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
        log.info(
            "StaffUserCreate({}) AppUser(username:{},email:{},role:{})",
            OperationalLogContext.PHASE_START,
            username,
            email,
            role
        );

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

        log.info(
            "StaffUserCreate({}) AppUser(id:{},username:{},role:{},active:{})",
            OperationalLogContext.PHASE_COMPLETE,
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.isActive()
        );
    }

}
