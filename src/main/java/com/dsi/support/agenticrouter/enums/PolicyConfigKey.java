package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Function;

@Getter
public enum PolicyConfigKey {
    AUTO_ROUTE_THRESHOLD("Minimum confidence threshold for auto-routing"),
    CRITICAL_MIN_CONF("Minimum confidence for CRITICAL priority tickets"),
    ROUTER_REPAIR_MAX_RETRIES("Max JSON repair retries before HUMAN_REVIEW"),
    SLA_ASSIGNED_HOURS_HIGH("Hours before reminding when assigned/in-progress high priority"),
    WAITING_CUSTOMER_REMINDER_HOURS("Hours before reminding customer when waiting on customer response"),
    INACTIVITY_AUTO_CLOSE_DAYS("Days of inactivity before auto-close workflow"),
    SLA_CUSTOMER_RESPONSE_HOURS("Hours to wait for customer response before SLA breach"),
    SLA_AGENT_RESPONSE_HOURS("Hours before agent response SLA breach"),
    AUTO_CLOSE_WARNING_DAYS("Days of inactivity before auto-close warning"),
    AUTO_CLOSE_FINAL_DAYS("Days of inactivity before auto-close resolved tickets"),
    MAX_ATTACHMENT_BYTES("Max attachment size in bytes"),
    AUTO_CLOSE_ENABLED("Enable inactivity auto-close workflow"),
    DEFAULT_QUEUE("Default routing queue key"),
    ROUTER_MODEL_PARAMS("Model parameters for router inference");

    private final String description;

    PolicyConfigKey(String description) {
        this.description = description;
    }

    private static <T> T getValue(
        BigDecimal value,
        T defaultValue,
        Function<BigDecimal, T> converter
    ) {
        if (Objects.isNull(value)) {
            return defaultValue;
        }

        return converter.apply(value);
    }

    public static int getIntValue(
        BigDecimal value,
        int defaultValue
    ) {
        return getValue(
            value,
            defaultValue,
            BigDecimal::intValue
        );
    }

    public static BigDecimal getBigDecimalValue(
        BigDecimal value,
        BigDecimal defaultValue
    ) {
        return getValue(
            value,
            defaultValue,
            Function.identity()
        );
    }

}
