package com.dsi.support.agenticrouter.service.policy;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.PolicyConfigKey;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.PolicyConfigRepository;
import com.dsi.support.agenticrouter.util.BindValidation;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyConfigService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final PolicyConfigRepository policyConfigRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<PolicyConfig> getAllPolicies() {
        List<PolicyConfig> policies = policyConfigRepository.findAllByOrderByConfigKey();

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
    ) throws BindException {
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

        PolicyConfig policyConfig = findPolicyConfig(
            policyConfigKey
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

    @Transactional
    public void updatePolicyActiveStatus(
        PolicyConfigKey policyConfigKey,
        boolean active
    ) {
        PolicyConfig policyConfig = findPolicyConfig(
            policyConfigKey
        );

        policyConfig.setActive(active);
        policyConfigRepository.save(policyConfig);

        log.info(
            "PolicyConfigStatusUpdate({}) PolicyConfig(key:{}) Outcome(active:{})",
            OperationalLogContext.PHASE_COMPLETE,
            policyConfig.getConfigKey(),
            policyConfig.isActive()
        );
    }

    @Transactional
    public void resetPolicyToDefault(
        PolicyConfigKey policyConfigKey
    ) throws BindException {
        PolicyConfig policyConfig = findPolicyConfig(
            policyConfigKey
        );

        if (Objects.isNull(policyConfig.getDefaultValue())) {
            throw BindValidation.fieldError(
                "policyResetRequest",
                "configKey",
                "Default value is not configured for " + policyConfigKey
            );
        }

        policyConfig.setConfigValue(
            policyConfig.getDefaultValue()
        );

        policyConfigRepository.save(policyConfig);

        log.info(
            "PolicyConfigReset({}) PolicyConfig(key:{}) Outcome(value:{})",
            OperationalLogContext.PHASE_COMPLETE,
            policyConfig.getConfigKey(),
            policyConfig.getConfigValue()
        );
    }

    private void validatePolicyConfigValue(
        PolicyConfigKey policyConfigKey,
        BigDecimal policyConfigValue
    ) throws BindException {
        switch (policyConfigKey) {
            case AUTO_ROUTE_THRESHOLD, CRITICAL_MIN_CONF -> validateBetweenZeroAndOne(
                policyConfigKey,
                policyConfigValue
            );
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
                 MAX_QUESTIONS_PER_TICKET -> validateAtLeastOne(
                policyConfigKey,
                policyConfigValue
            );
            case AUTO_CLOSE_ENABLED,
                 AUTONOMOUS_ENABLED,
                 FRUSTRATION_DETECTION_ENABLED,
                 LOOP_DETECTION_ENABLED -> validateBooleanFlagValue(
                policyConfigKey,
                policyConfigValue
            );
            case DEFAULT_QUEUE, ROUTER_MODEL_PARAMS -> validateNonNegative(
                policyConfigKey,
                policyConfigValue
            );
        }
    }

    private void validateBetweenZeroAndOne(
        PolicyConfigKey policyConfigKey,
        BigDecimal policyConfigValue
    ) throws BindException {
        if (policyConfigValue.compareTo(ZERO) < 0 || policyConfigValue.compareTo(ONE) > 0) {
            throw BindValidation.fieldError(
                "policyUpdateRequest",
                "configValue",
                policyConfigKey + " must be between 0 and 1."
            );
        }
    }

    private void validateAtLeastOne(
        PolicyConfigKey policyConfigKey,
        BigDecimal policyConfigValue
    ) throws BindException {
        if (policyConfigValue.compareTo(ONE) < 0) {
            throw BindValidation.fieldError(
                "policyUpdateRequest",
                "configValue",
                policyConfigKey + " must be greater than or equal to 1."
            );
        }
    }

    private void validateBooleanFlagValue(
        PolicyConfigKey policyConfigKey,
        BigDecimal policyConfigValue
    ) throws BindException {
        if (policyConfigValue.compareTo(ZERO) != 0 && policyConfigValue.compareTo(ONE) != 0) {
            throw BindValidation.fieldError(
                "policyUpdateRequest",
                "configValue",
                policyConfigKey + " must be 0 or 1."
            );
        }
    }

    private void validateNonNegative(
        PolicyConfigKey policyConfigKey,
        BigDecimal policyConfigValue
    ) throws BindException {
        if (policyConfigValue.compareTo(ZERO) < 0) {
            throw BindValidation.fieldError(
                "policyUpdateRequest",
                "configValue",
                policyConfigKey + " must be non-negative."
            );
        }
    }

    private PolicyConfig findPolicyConfig(
        PolicyConfigKey policyConfigKey
    ) {
        return policyConfigRepository.findByConfigKey(policyConfigKey)
                                     .orElseThrow(
                                         DataNotFoundException.supplier(
                                             PolicyConfig.class,
                                             policyConfigKey
                                         )
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
    ) throws BindException {
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
            throw BindValidation.fieldError(
                "staffCreateRequest",
                "username",
                "Username is required"
            );
        }

        if (StringUtils.isBlank(normalizedEmail)) {
            throw BindValidation.fieldError(
                "staffCreateRequest",
                "email",
                "Email is required"
            );
        }

        if (StringUtils.isBlank(normalizedFullName)) {
            throw BindValidation.fieldError(
                "staffCreateRequest",
                "fullName",
                "Full name is required"
            );
        }

        if (appUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw BindValidation.fieldError(
                "staffCreateRequest",
                "username",
                "Username already exists: " + normalizedUsername
            );
        }

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw BindValidation.fieldError(
                "staffCreateRequest",
                "email",
                "Email already exists: " + normalizedEmail
            );
        }

        if (!role.canAccessAgentPortal()) {
            throw BindValidation.fieldError(
                "staffCreateRequest",
                "role",
                "Role must be staff role (AGENT, SUPERVISOR, ADMIN)"
            );
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
