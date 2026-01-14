package com.dsi.support.agenticrouter.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class MvcExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(MvcExceptionHandler.class);

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDenied(Exception ex) {
        logger.warn("Access denied: ", ex);

        return new ModelAndView("error/403");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleMvcException(Exception ex) {
        logger.error("Exception occurred: ", ex);

        return new ModelAndView("error/500");
    }
}
