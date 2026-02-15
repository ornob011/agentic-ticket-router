package com.dsi.support.agenticrouter.exception;

import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    RESOURCE_NOT_FOUND(
        "RESOURCE_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "The requested resource was not found"
    ),
    DATA_NOT_FOUND(
        "DATA_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Data not found"
    ),

    VALIDATION_ERROR(
        "VALIDATION_ERROR",
        HttpStatus.BAD_REQUEST,
        "Validation failed"
    ),
    MALFORMED_REQUEST(
        "MALFORMED_REQUEST",
        HttpStatus.BAD_REQUEST,
        "Request body is malformed or invalid"
    ),
    MISSING_PARAMETER(
        "MISSING_PARAMETER",
        HttpStatus.BAD_REQUEST,
        "Required parameter is missing"
    ),
    INVALID_PARAMETER_TYPE(
        "INVALID_PARAMETER_TYPE",
        HttpStatus.BAD_REQUEST,
        "Parameter has invalid type"
    ),
    MISSING_PATH_VARIABLE(
        "MISSING_PATH_VARIABLE",
        HttpStatus.BAD_REQUEST,
        "Required path variable is missing"
    ),

    UNAUTHORIZED(
        "UNAUTHORIZED",
        HttpStatus.UNAUTHORIZED,
        "Authentication required"
    ),
    FORBIDDEN(
        "FORBIDDEN",
        HttpStatus.FORBIDDEN,
        "Access denied"
    ),
    OPERATION_NOT_ALLOWED(
        "OPERATION_NOT_ALLOWED",
        HttpStatus.FORBIDDEN,
        "This operation is not allowed"
    ),

    CONFLICT(
        "CONFLICT",
        HttpStatus.CONFLICT,
        "Resource conflict"
    ),
    RESOURCE_ALREADY_EXISTS(
        "RESOURCE_ALREADY_EXISTS",
        HttpStatus.CONFLICT,
        "Resource already exists"
    ),

    BUSINESS_RULE_VIOLATION(
        "BUSINESS_RULE_VIOLATION",
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Business rule violation"
    ),

    METHOD_NOT_ALLOWED(
        "METHOD_NOT_ALLOWED",
        HttpStatus.METHOD_NOT_ALLOWED,
        "HTTP method not allowed"
    ),
    ENDPOINT_NOT_FOUND(
        "ENDPOINT_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Endpoint not found"
    ),

    INTERNAL_ERROR(
        "INTERNAL_ERROR",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred"
    ),


    ;

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    public String getTypeUri() {
        String normalizedCode = StringUtils.replaceChars(
            StringNormalizationUtils.lowerTrimmedOrEmpty(code),
            '_',
            '-'
        );

        return UriComponentsBuilder.fromPath("/errors/{code}")
                                   .buildAndExpand(normalizedCode)
                                   .toUriString();
    }
}
