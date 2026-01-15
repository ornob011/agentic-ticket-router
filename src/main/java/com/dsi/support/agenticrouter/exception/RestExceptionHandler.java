package com.dsi.support.agenticrouter.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(annotations = RestController.class)
public class RestExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleRestException(
        Exception exception,
        HttpServletRequest request
    ) {
        logger.error("Exception occurred: ", exception);

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
        logger.warn("DataNotFoundException: ", exception);

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
        logger.warn("Access denied: ", exception);

        HttpStatus status = HttpStatus.FORBIDDEN;
        ProblemDetail problem = ProblemDetail.forStatus(status);

        problem.setTitle("Forbidden");
        problem.setDetail("You do not have permission to perform this action.");
        problem.setInstance(URI.create(request.getRequestURI()));

        return problem;
    }

}
