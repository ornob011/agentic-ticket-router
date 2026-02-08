package com.dsi.support.agenticrouter.validator;

import com.dsi.support.agenticrouter.entity.PolicyConfig;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Objects;

public class PolicyConfigValidator implements ConstraintValidator<ValidPolicyConfig, PolicyConfig> {

    private static boolean isInRange(
        BigDecimal configValue,
        BigDecimal minValue,
        BigDecimal maxValue
    ) {
        if (Objects.isNull(configValue)) {
            return false;
        }

        if (Objects.nonNull(minValue) && configValue.compareTo(minValue) < 0) {
            return false;
        }

        return !Objects.nonNull(maxValue) || configValue.compareTo(maxValue) <= 0;
    }

    @Override
    public boolean isValid(PolicyConfig policyConfig, ConstraintValidatorContext context) {
        if (Objects.isNull(policyConfig)) {
            return true;
        }

        BigDecimal configValue = policyConfig.getConfigValue();
        BigDecimal minValue = policyConfig.getMinValue();
        BigDecimal maxValue = policyConfig.getMaxValue();

        if (Objects.isNull(configValue) && (Objects.nonNull(minValue) || Objects.nonNull(maxValue))) {
            return false;
        }

        if (Objects.nonNull(configValue)) {
            return isInRange(configValue, minValue, maxValue);
        }

        return true;
    }
}
