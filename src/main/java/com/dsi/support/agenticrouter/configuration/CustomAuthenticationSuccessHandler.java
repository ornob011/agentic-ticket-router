package com.dsi.support.agenticrouter.configuration;

import com.dsi.support.agenticrouter.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final List<String> ERROR_PATHS = List.of(
        "/error",
        "/access-denied"
    );

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {

        SavedRequest savedRequest = requestCache.getRequest(
            request,
            response
        );

        String targetUrl = Constants.HOME_URL;

        if (Objects.nonNull(savedRequest)) {
            String requestedUrl = savedRequest.getRedirectUrl();

            if (StringUtils.isNotBlank(requestedUrl) && !isErrorPage(requestedUrl)) {
                targetUrl = requestedUrl;
            }

            requestCache.removeRequest(
                request,
                response
            );
        }

        redirectStrategy.sendRedirect(
            request,
            response,
            targetUrl
        );
    }

    private boolean isErrorPage(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        for (String path : ERROR_PATHS) {
            if (url.contains(path)) {
                return true;
            }
        }

        return false;
    }

}
