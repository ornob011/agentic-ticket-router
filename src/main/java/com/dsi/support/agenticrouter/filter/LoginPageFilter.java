package com.dsi.support.agenticrouter.filter;

import com.dsi.support.agenticrouter.util.Constants;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Component
@Slf4j
public class LoginPageFilter extends GenericFilterBean {

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (Utils.isLoggedIn() && isLoginRequest(httpRequest)) {
            log.info(
                "LoginRedirect({}) HttpRequest(method:{},uri:{}) Outcome(action:{})",
                OperationalLogContext.PHASE_DECISION,
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                "redirect_home_for_authenticated_user"
            );

            httpResponse.sendRedirect(Constants.HOME_URL);
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return Constants.LOGIN_URL.equals(request.getRequestURI());
    }
}
