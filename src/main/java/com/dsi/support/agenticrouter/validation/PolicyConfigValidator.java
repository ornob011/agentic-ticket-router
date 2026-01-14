package com.dsi.support.agenticrouter.validation;

import com.dsi.support.agenticrouter.entity.PolicyConfig;
import com.dsi.support.agenticrouter.enums.ConfigValueType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Objects;

public class PolicyConfigValidator implements ConstraintValidator<ValidPolicyConfig, PolicyConfig> {

    private static boolean isBooleanLiteral(String configValue) {
        return "true".equalsIgnoreCase(configValue)
               || "false".equalsIgnoreCase(configValue);
    }

    private static boolean isDecimalInRange(
        String configValue,
        String minValue,
        String maxValue
    ) {
        BigDecimal numericValue = parseBigDecimal(configValue);

        if (Objects.isNull(numericValue)) {
            return false;
        }

        return isBigDecimalWithinRange(
            numericValue,
            minValue,
            maxValue
        );
    }

    private static boolean isIntegerInRange(
        String configValue,
        String minValue,
        String maxValue
    ) {
        Integer numericValue = parseInteger(configValue);

        if (Objects.isNull(numericValue)) {
            return false;
        }

        Integer min = parseIntegerNullable(minValue);
        Integer max = parseIntegerNullable(maxValue);

        if (Objects.nonNull(min) && numericValue < min) {
            return false;
        }

        return !Objects.nonNull(max) || numericValue <= max;
    }

    private static boolean isLongInRange(
        String configValue,
        String minValue,
        String maxValue
    ) {
        Long numericValue = parseLong(configValue);

        if (Objects.isNull(numericValue)) {
            return false;
        }

        Long min = parseLongNullable(minValue);
        Long max = parseLongNullable(maxValue);

        if (Objects.nonNull(min) && numericValue < min) {
            return false;
        }

        return !Objects.nonNull(max) || numericValue <= max;
    }

    private static boolean isBigDecimalWithinRange(
        BigDecimal numericValue,
        String minValue,
        String maxValue
    ) {
        BigDecimal min = parseBigDecimalNullable(minValue);

        if (Objects.nonNull(min)
            && numericValue.compareTo(min) < 0) {
            return false;
        }

        BigDecimal max = parseBigDecimalNullable(maxValue);

        return !Objects.nonNull(max)
               || numericValue.compareTo(max) <= 0;
    }

    private static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimalNullable(String value) {
        if (Objects.isNull(value)) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return parseBigDecimal(trimmed);
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseIntegerNullable(String value) {
        if (Objects.isNull(value)) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return parseInteger(trimmed);
    }

    private static Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long parseLongNullable(String value) {
        if (Objects.isNull(value)) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return parseLong(trimmed);
    }

    @Override
    public boolean isValid(PolicyConfig policyConfig, ConstraintValidatorContext context) {
        if (Objects.isNull(policyConfig)) {
            return true;
        }

        ConfigValueType valueType = policyConfig.getValueType();
        String rawValue = policyConfig.getConfigValue();

        if (Objects.isNull(valueType) || Objects.isNull(rawValue)) {
            return false;
        }

        String configValue = rawValue.trim();

        if (configValue.isEmpty()) {
            return false;
        }

        return switch (valueType) {
            case DOUBLE -> isDecimalInRange(
                configValue,
                policyConfig.getMinValue(),
                policyConfig.getMaxValue()
            );

            case INTEGER -> isIntegerInRange(
                configValue,
                policyConfig.getMinValue(),
                policyConfig.getMaxValue()
            );

            case LONG -> isLongInRange(
                configValue,
                policyConfig.getMinValue(),
                policyConfig.getMaxValue()
            );

            case BOOLEAN -> isBooleanLiteral(
                configValue
            );

            case STRING, JSON -> true;
        };
    }
}
