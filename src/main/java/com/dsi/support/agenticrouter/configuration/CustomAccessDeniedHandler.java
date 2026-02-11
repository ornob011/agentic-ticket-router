package com.dsi.support.agenticrouter.configuration;

import com.dsi.support.agenticrouter.util.OperationalLogContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        String requestURI = request.getRequestURI();

        String method = request.getMethod();

        String queryString = request.getQueryString();

        String fullURL = String.format("%s%s",
            requestURI,
            StringUtils.isBlank(queryString) ? StringUtils.EMPTY : "?" + queryString
        );

        log.warn(
            "AccessDeniedHandle({}) HttpRequest(method:{},uri:{}) Outcome(action:{})",
            OperationalLogContext.PHASE_FAIL,
            method,
            fullURL,
            "redirect_access_denied"
        );

        response.sendRedirect("/access-denied");
    }
}
