package com.dsi.support.agenticrouter.controller.mvc;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Objects;

@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @GetMapping("/error")
    public String handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        if (Objects.nonNull(statusCode)) {
            log.error(
                "ErrorRoute(fail) HttpRequest(uri:{}) Outcome(statusCode:{},message:{})",
                request.getRequestURI(),
                statusCode,
                message
            );
        } else {
            log.error(
                "ErrorRoute(fail) HttpRequest(uri:{}) Outcome(reason:{})",
                request.getRequestURI(),
                "unknown_error"
            );
        }

        return "error/500";
    }
}
