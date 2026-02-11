package com.dsi.support.agenticrouter.exception;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(annotations = RestController.class)
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleRestException(
        Exception exception,
        HttpServletRequest request
    ) {
        log.error(
            "RestExceptionHandle({}) HttpRequest(method:{},uri:{}) Outcome(exceptionType:{},message:{})",
            OperationalLogContext.PHASE_FAIL,
            request.getMethod(),
            request.getRequestURI(),
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception
        );

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ProblemDetail problem = ProblemDetail.forStatus(status);

        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));

        return problem;
    }

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

        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail problem = ProblemDetail.forStatus(status);

        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));

        return problem;
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

        HttpStatus status = HttpStatus.FORBIDDEN;
        ProblemDetail problem = ProblemDetail.forStatus(status);

        problem.setTitle("Forbidden");
        problem.setDetail("You do not have permission to perform this action.");
        problem.setInstance(URI.create(request.getRequestURI()));

        return problem;
    }

}
