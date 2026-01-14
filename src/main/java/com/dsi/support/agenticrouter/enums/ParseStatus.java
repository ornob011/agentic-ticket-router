package com.dsi.support.agenticrouter.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

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

    private final String description;

    ParseStatus(String description) {
        this.description = description;
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
