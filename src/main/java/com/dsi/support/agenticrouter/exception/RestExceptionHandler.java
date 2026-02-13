package com.dsi.support.agenticrouter.exception;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(annotations = RestController.class)
@Slf4j
@RequiredArgsConstructor
public class RestExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(DataNotFoundException.class)
    public ProblemDetail handleDataNotFoundException(
        DataNotFoundException exception,
        HttpServletRequest request
    ) {
        log.warn(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(exceptionType:{},message:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception
        );

        return buildProblem(
            request,
            HttpStatus.NOT_FOUND,
            "Not Found",
            exception.getMessage()
        );
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ProblemDetail handleAccessDenied(
        Exception exception,
        HttpServletRequest request
    ) {
        log.warn(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(exceptionType:{},message:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception
        );

        return buildProblem(
            request,
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "You do not have permission to perform this action."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<FieldError> fieldErrorList = exception.getBindingResult().getFieldErrors();
        List<String> globalErrors = exception.getBindingResult()
                                             .getGlobalErrors()
                                             .stream()
                                             .map(error -> messageSource.getMessage(error, locale))
                                             .toList();

        Map<String, String> fieldErrors = fieldErrorList.stream()
                                                        .collect(
                                                            Collectors.toMap(
                                                                FieldError::getField,
                                                                fieldError -> messageSource.getMessage(fieldError, locale),
                                                                (first, second) -> first,
                                                                LinkedHashMap::new
                                                            )
                                                        );

        List<String> errors = fieldErrorList.stream()
                                            .map(fieldError -> messageSource.getMessage(fieldError, locale))
                                            .toList();

        log.warn(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(exceptionType:{},fieldErrorsCount:{},globalErrorsCount:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            exception.getClass().getSimpleName(),
            fieldErrors.size(),
            globalErrors.size(),
            exception
        );

        ProblemDetail problemDetail = buildProblem(
            request,
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            "One or more fields are invalid."
        );

        problemDetail.setProperty("fieldErrors", fieldErrors);
        problemDetail.setProperty("globalErrors", globalErrors);
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    @ExceptionHandler(BindException.class)
    public ProblemDetail handleBindException(
        BindException exception,
        HttpServletRequest request
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<FieldError> fieldErrorList = exception.getBindingResult().getFieldErrors();
        List<String> globalErrors = exception.getBindingResult()
                                             .getGlobalErrors()
                                             .stream()
                                             .map(error -> messageSource.getMessage(error, locale))
                                             .toList();

        Map<String, String> fieldErrors = fieldErrorList.stream()
                                                        .collect(
                                                            Collectors.toMap(
                                                                FieldError::getField,
                                                                fieldError -> messageSource.getMessage(fieldError, locale),
                                                                (first, second) -> first,
                                                                LinkedHashMap::new
                                                            )
                                                        );

        List<String> errors = fieldErrorList.stream()
                                            .map(fieldError -> messageSource.getMessage(fieldError, locale))
                                            .toList();

        log.warn(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(exceptionType:{},fieldErrorsCount:{},globalErrorsCount:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            exception.getClass().getSimpleName(),
            fieldErrors.size(),
            globalErrors.size(),
            exception
        );

        ProblemDetail problemDetail = buildProblem(
            request,
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            "One or more fields are invalid."
        );

        problemDetail.setProperty("fieldErrors", fieldErrors);
        problemDetail.setProperty("globalErrors", globalErrors);
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        log.warn(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(exceptionType:{},message:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception
        );

        return buildProblem(
            request,
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleRestException(
        Exception exception,
        HttpServletRequest request
    ) {
        log.error(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(exceptionType:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            exception.getClass().getSimpleName(),
            exception
        );

        return buildProblem(
            request,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred."
        );
    }

    private ProblemDetail buildProblem(
        HttpServletRequest request,
        HttpStatus status,
        String title,
        String detail
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return problemDetail;
    }
}
