package com.dsi.support.agenticrouter.exception;

import com.dsi.support.agenticrouter.enums.ErrorCode;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice(annotations = RestController.class)
@Slf4j
@RequiredArgsConstructor
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_ERROR_DETAIL = "One or more fields are invalid.";
    private static final String INTERNAL_ERROR_DETAIL = "An unexpected error occurred. Please try again later.";
    private static final String INVALID_CREDENTIALS_DETAIL = "Invalid username or password.";

    private final MessageSource messageSource;

    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplicationException(
        ApplicationException exception,
        HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        logWarn(
            request,
            errorCode,
            exception,
            exception.getMessage()
        );

        return buildProblemDetail(
            request,
            errorCode,
            exception.getMessage()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = exception.getConstraintViolations()
                                                   .stream()
                                                   .collect(Collectors.toMap(
                                                       this::extractPropertyName,
                                                       ConstraintViolation::getMessage,
                                                       (first, second) -> first,
                                                       LinkedHashMap::new
                                                   ));

        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.VALIDATION_ERROR,
            VALIDATION_ERROR_DETAIL
        );

        problemDetail.setProperty("fieldErrors", fieldErrors);

        logWarn(
            request,
            ErrorCode.VALIDATION_ERROR,
            exception,
            "violations=" + fieldErrors.size()
        );

        return problemDetail;
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ProblemDetail handleAccessDenied(
        Exception exception,
        HttpServletRequest request
    ) {
        logWarn(
            request,
            ErrorCode.FORBIDDEN,
            exception,
            "accessDenied"
        );

        return buildProblemDetail(
            request,
            ErrorCode.FORBIDDEN,
            "You do not have permission to perform this action."
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(
        BadCredentialsException exception,
        HttpServletRequest request
    ) {
        logWarn(
            request,
            ErrorCode.UNAUTHORIZED,
            exception,
            "badCredentials"
        );

        return buildProblemDetail(
            request,
            ErrorCode.UNAUTHORIZED,
            INVALID_CREDENTIALS_DETAIL
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        logWarn(
            request,
            ErrorCode.VALIDATION_ERROR,
            exception,
            exception.getMessage()
        );

        return buildProblemDetail(
            request,
            ErrorCode.VALIDATION_ERROR,
            exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(
        Exception exception,
        HttpServletRequest request
    ) {
        logError(
            request,
            ErrorCode.INTERNAL_ERROR,
            exception,
            "unhandled"
        );

        return buildProblemDetail(
            request,
            ErrorCode.INTERNAL_ERROR,
            INTERNAL_ERROR_DETAIL
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        @NonNull MethodArgumentNotValidException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, String> fieldErrors = toFieldErrors(
            exception.getBindingResult().getFieldErrors()
        );

        List<String> globalErrors = exception.getBindingResult()
                                             .getGlobalErrors()
                                             .stream()
                                             .map(error -> messageSource.getMessage(error, locale))
                                             .toList();

        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.VALIDATION_ERROR,
            VALIDATION_ERROR_DETAIL
        );

        problemDetail.setProperty("fieldErrors", fieldErrors);
        problemDetail.setProperty("globalErrors", globalErrors);

        logWarn(
            request,
            ErrorCode.VALIDATION_ERROR,
            exception,
            "fieldErrors=" + fieldErrors.size() + ",globalErrors=" + globalErrors.size()
        );

        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                             .body(problemDetail);
    }

    @ExceptionHandler(BindException.class)
    public ProblemDetail handleBindException(
        BindException exception,
        HttpServletRequest request
    ) {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, String> fieldErrors = toFieldErrors(
            exception.getBindingResult().getFieldErrors()
        );
        List<String> globalErrors = exception.getBindingResult()
                                             .getGlobalErrors()
                                             .stream()
                                             .map(error -> messageSource.getMessage(error, locale))
                                             .toList();

        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.VALIDATION_ERROR,
            VALIDATION_ERROR_DETAIL
        );

        problemDetail.setProperty("fieldErrors", fieldErrors);
        problemDetail.setProperty("globalErrors", globalErrors);

        logWarn(
            request,
            ErrorCode.VALIDATION_ERROR,
            exception,
            "fieldErrors=" + fieldErrors.size() + ",globalErrors=" + globalErrors.size()
        );

        return problemDetail;
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
        @NonNull TypeMismatchException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        String parameterName = Objects.nonNull(exception.getPropertyName()) ? exception.getPropertyName() : "unknown";

        Class<?> requiredTypeClass = exception.getRequiredType();

        String requiredType = Objects.nonNull(requiredTypeClass)
            ? requiredTypeClass.getSimpleName()
            : "unknown";

        if (exception instanceof MethodArgumentTypeMismatchException mismatchException) {
            parameterName = mismatchException.getName();
        }

        String detail = String.format(
            "Parameter '%s' should be of type '%s'.",
            parameterName,
            requiredType
        );
        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.INVALID_PARAMETER_TYPE,
            detail
        );

        logWarn(
            request,
            ErrorCode.INVALID_PARAMETER_TYPE,
            exception,
            "parameter=" + parameterName + ",requiredType=" + requiredType
        );

        return ResponseEntity.status(ErrorCode.INVALID_PARAMETER_TYPE.getStatus())
                             .body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        @NonNull HttpMessageNotReadableException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        String detail = "Request body is malformed or could not be read.";

        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.MALFORMED_REQUEST,
            detail
        );

        logWarn(
            request,
            ErrorCode.MALFORMED_REQUEST,
            exception,
            "body=malformed"
        );

        return ResponseEntity.status(ErrorCode.MALFORMED_REQUEST.getStatus())
                             .body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
        @NonNull MissingServletRequestParameterException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        String detail = String.format(
            "Required parameter '%s' is missing.",
            exception.getParameterName()
        );

        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.MISSING_PARAMETER,
            detail
        );

        logWarn(
            request,
            ErrorCode.MISSING_PARAMETER,
            exception,
            "parameter=" + exception.getParameterName()
        );

        return ResponseEntity.status(ErrorCode.MISSING_PARAMETER.getStatus())
                             .body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(
        @NonNull MissingPathVariableException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        String detail = String.format(
            "Required path variable '%s' is missing.",
            exception.getVariableName()
        );

        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.MISSING_PATH_VARIABLE,
            detail
        );

        logWarn(
            request,
            ErrorCode.MISSING_PATH_VARIABLE,
            exception,
            "variable=" + exception.getVariableName()
        );

        return ResponseEntity.status(ErrorCode.MISSING_PATH_VARIABLE.getStatus())
                             .body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
        @NonNull HttpRequestMethodNotSupportedException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        String detail = String.format(
            "HTTP method '%s' is not supported for this endpoint.",
            exception.getMethod()
        );

        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.METHOD_NOT_ALLOWED,
            detail
        );

        logWarn(
            request,
            ErrorCode.METHOD_NOT_ALLOWED,
            exception,
            "supportedMethods=" + exception.getSupportedHttpMethods()
        );

        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                             .body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
        @NonNull HttpMediaTypeNotSupportedException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        String detail = String.format(
            "Media type '%s' is not supported.",
            exception.getContentType()
        );

        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.MALFORMED_REQUEST,
            detail
        );

        logWarn(
            request,
            ErrorCode.MALFORMED_REQUEST,
            exception,
            "contentType=" + exception.getContentType()
        );

        return ResponseEntity.status(ErrorCode.MALFORMED_REQUEST.getStatus())
                             .body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
        @NonNull NoHandlerFoundException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        ProblemDetail problemDetail = buildProblemDetail(
            request,
            ErrorCode.ENDPOINT_NOT_FOUND,
            "The requested endpoint was not found."
        );

        logWarn(
            request,
            ErrorCode.ENDPOINT_NOT_FOUND,
            exception,
            "endpointNotFound"
        );

        return ResponseEntity.status(ErrorCode.ENDPOINT_NOT_FOUND.getStatus())
                             .body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        @NonNull Exception exception,
        @Nullable Object body,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode statusCode,
        @NonNull WebRequest request
    ) {
        if (body instanceof ProblemDetail existingProblemDetail) {
            ErrorCode errorCode = resolveErrorCode(statusCode);

            existingProblemDetail.setType(URI.create(errorCode.getTypeUri()));
            existingProblemDetail.setProperty("code", errorCode.getCode());
            existingProblemDetail.setProperty("timestamp", Instant.now());
            existingProblemDetail.setInstance(URI.create(resolveRequestUri(request)));
        }

        return super.handleExceptionInternal(
            exception,
            body,
            headers,
            statusCode,
            request
        );
    }

    private Map<String, String> toFieldErrors(
        List<FieldError> fieldErrors
    ) {
        return fieldErrors.stream()
                          .collect(
                              Collectors.toMap(
                                  FieldError::getField,
                                  fieldError -> messageSource.getMessage(fieldError, LocaleContextHolder.getLocale()),
                                  (first, second) -> first,
                                  LinkedHashMap::new
                              )
                          );
    }

    private ProblemDetail buildProblemDetail(
        HttpServletRequest request,
        ErrorCode errorCode,
        String detail
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail);

        problemDetail.setTitle(errorCode.getStatus().getReasonPhrase());
        problemDetail.setType(URI.create(errorCode.getTypeUri()));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", errorCode.getCode());
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    private ProblemDetail buildProblemDetail(
        WebRequest request,
        ErrorCode errorCode,
        String detail
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail);

        problemDetail.setTitle(errorCode.getStatus().getReasonPhrase());
        problemDetail.setType(URI.create(errorCode.getTypeUri()));
        problemDetail.setInstance(URI.create(resolveRequestUri(request)));
        problemDetail.setProperty("code", errorCode.getCode());
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    private ErrorCode resolveErrorCode(
        HttpStatusCode statusCode
    ) {
        int statusValue = statusCode.value();

        return switch (statusValue) {
            case 400 -> ErrorCode.VALIDATION_ERROR;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.ENDPOINT_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    private String resolveRequestUri(
        WebRequest request
    ) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }

        return StringUtils.EMPTY;
    }

    private void logWarn(
        HttpServletRequest request,
        ErrorCode errorCode,
        Exception exception,
        String details
    ) {
        log.warn(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(code:{},details:{},exception:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            errorCode.getCode(),
            details,
            exception.getClass().getSimpleName(),
            exception
        );
    }

    private void logWarn(
        WebRequest request,
        ErrorCode errorCode,
        Exception exception,
        String details
    ) {
        log.warn(
            "RestExceptionHandle({}) HttpRequest(uri:{}) Outcome(code:{},details:{},exception:{})",
            OperationalLogContext.PHASE_FAIL,
            resolveRequestUri(request),
            errorCode.getCode(),
            details,
            exception.getClass().getSimpleName(),
            exception
        );
    }

    private void logError(
        HttpServletRequest request,
        ErrorCode errorCode,
        Exception exception,
        String details
    ) {
        log.error(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(code:{},details:{},exception:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            errorCode.getCode(),
            details,
            exception.getClass().getSimpleName(),
            exception
        );
    }

    private String extractPropertyName(
        ConstraintViolation<?> violation
    ) {
        String propertyPath = violation.getPropertyPath().toString();

        return StringUtils.substringAfterLast(
            propertyPath,
            "."
        );
    }
}
