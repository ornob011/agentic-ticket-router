package com.dsi.support.agenticrouter.enums;

import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Getter
public enum ParseStatus {
    SUCCESS("Successfully parsed and validated"),
    INVALID_JSON("Invalid JSON format"),
    INVALID_ENUM("Valid JSON but contains invalid enum values"),
    SCHEMA_VIOLATION("Valid JSON but violates expected schema"),
    REPAIR_ATTEMPTED("Repair prompt sent to model"),
    REPAIR_SUCCESS("Successfully repaired after retry"),
    REPAIR_FAILED("Repair attempts exhausted, human review needed"),
    TIMEOUT("Model inference timed out"),
    MODEL_ERROR("Model returned error response");

    private static final Set<ParseStatus> SUCCESS_STATUSES =
        EnumSet.of(
            SUCCESS,
            REPAIR_SUCCESS
        );

    private static final Set<ParseStatus> HUMAN_REVIEW_REQUIRED =
        EnumSet.of(
            REPAIR_FAILED,
            SCHEMA_VIOLATION,
            MODEL_ERROR
        );

    private static final Set<ParseStatus> RETRYABLE =
        EnumSet.of(
            INVALID_JSON,
            INVALID_ENUM
        );

    private static final List<Class<? extends Throwable>> TIMEOUT_TYPES =
        Arrays.asList(
            TimeoutException.class,
            HttpTimeoutException.class,
            SocketTimeoutException.class,
            InterruptedIOException.class
        );

    private final String description;

    ParseStatus(String description) {
        this.description = description;
    }

    public static boolean isTimeout(
        Throwable throwable
    ) {
        for (Throwable cause : ExceptionUtils.getThrowableList(throwable)) {
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                return true;
            }

            for (Class<? extends Throwable> timeoutType : TIMEOUT_TYPES) {
                if (timeoutType.isInstance(cause)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isSuccess() {
        return SUCCESS_STATUSES.contains(this);
    }

    public boolean requiresHumanReview() {
        return HUMAN_REVIEW_REQUIRED.contains(this);
    }

    public boolean canRetry() {
        return RETRYABLE.contains(this);
    }
}
