package com.dsi.support.agenticrouter.service.policy;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
                                                          .orElseThrow(
                                                              DataNotFoundException.supplier(
                                                                  PolicyConfig.class,
                                                                  policyConfigKey
                                                              )
                                                          );

        log.debug(
            "PolicyConfigLookup({}) PolicyConfig(key:{},activeFound:{})",
            OperationalLogContext.PHASE_COMPLETE,
            policyConfigKey,
            true
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
        validatePolicyConfigValue(
            policyConfigKey,
            policyConfigValue
        );

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

    private void validatePolicyConfigValue(
        PolicyConfigKey policyConfigKey,
        BigDecimal policyConfigValue
    ) {
        switch (policyConfigKey) {
            case AUTO_ROUTE_THRESHOLD, CRITICAL_MIN_CONF -> {
                if (policyConfigValue.compareTo(BigDecimal.ZERO) < 0
                    || policyConfigValue.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalArgumentException(
                        policyConfigKey + " must be between 0 and 1."
                    );
                }
            }
            case ROUTER_REPAIR_MAX_RETRIES,
                 SLA_ASSIGNED_HOURS_HIGH,
                 WAITING_CUSTOMER_REMINDER_HOURS,
                 INACTIVITY_AUTO_CLOSE_DAYS,
                 SLA_CUSTOMER_RESPONSE_HOURS,
                 SLA_AGENT_RESPONSE_HOURS,
                 AUTO_CLOSE_WARNING_DAYS,
                 AUTO_CLOSE_FINAL_DAYS,
                 MAX_ATTACHMENT_BYTES,
                 MAX_AUTONOMOUS_ACTIONS,
                 MAX_QUESTIONS_PER_TICKET -> {
                if (policyConfigValue.compareTo(BigDecimal.ONE) < 0) {
                    throw new IllegalArgumentException(
                        policyConfigKey + " must be greater than or equal to 1."
                    );
                }
            }
            case AUTO_CLOSE_ENABLED,
                 AUTONOMOUS_ENABLED,
                 FRUSTRATION_DETECTION_ENABLED,
                 LOOP_DETECTION_ENABLED -> {
                boolean isZero = policyConfigValue.compareTo(BigDecimal.ZERO) == 0;
                boolean isOne = policyConfigValue.compareTo(BigDecimal.ONE) == 0;
                if (!(isZero || isOne)) {
                    throw new IllegalArgumentException(
                        policyConfigKey + " must be 0 or 1."
                    );
                }
            }
            case DEFAULT_QUEUE, ROUTER_MODEL_PARAMS -> {
                if (policyConfigValue.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException(
                        policyConfigKey + " must be non-negative."
                    );
                }
            }
        }
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

        String normalizedUsername = StringNormalizationUtils.lowerTrimmedOrNull(username);
        String normalizedEmail = StringNormalizationUtils.lowerTrimmedOrNull(email);
        String normalizedFullName = StringNormalizationUtils.trimToNull(fullName);

        if (StringUtils.isBlank(normalizedUsername)) {
            throw new IllegalArgumentException("Username is required");
        }

        if (StringUtils.isBlank(normalizedEmail)) {
            throw new IllegalArgumentException("Email is required");
        }

        if (StringUtils.isBlank(normalizedFullName)) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (appUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists: " + normalizedUsername);
        }

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists: " + normalizedEmail);
        }

        if (!role.canAccessAgentPortal()) {
            throw new IllegalArgumentException("Role must be staff role (AGENT, SUPERVISOR, ADMIN)");
        }

        AppUser user = AppUser.builder()
                              .username(normalizedUsername)
                              .email(normalizedEmail)
                              .fullName(normalizedFullName)
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
