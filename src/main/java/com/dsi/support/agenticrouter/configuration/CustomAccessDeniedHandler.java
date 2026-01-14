package com.dsi.support.agenticrouter.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exc
    ) throws IOException {
        String requestURI = request.getRequestURI();

        String method = request.getMethod();

        String queryString = request.getQueryString();

        String fullURL = String.format("%s%s",
            requestURI,
            StringUtils.isBlank(queryString) ? StringUtils.EMPTY : "?" + queryString
        );

        logger.info("Access denied for request: {} {}", method, fullURL);

        response.sendRedirect("/access-denied");
    }
}
