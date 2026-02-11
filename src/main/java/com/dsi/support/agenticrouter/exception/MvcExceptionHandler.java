package com.dsi.support.agenticrouter.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(basePackages = {
    "com.dsi.support.agenticrouter.controller.mvc"
})
@Slf4j
public class MvcExceptionHandler {

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDenied(Exception exception) {
        log.warn(
            "MvcExceptionHandle(fail) Outcome(exceptionType:{},message:{},view:{})",
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            "error/403",
            exception
        );

        return new ModelAndView("error/403");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleMvcException(Exception exception) {
        log.error(
            "MvcExceptionHandle(fail) Outcome(exceptionType:{},message:{},view:{})",
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            "error/500",
            exception
        );

        return new ModelAndView("error/500");
    }
}
