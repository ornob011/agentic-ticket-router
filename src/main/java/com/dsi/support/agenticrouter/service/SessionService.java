package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.util.Constants;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;

@Service
public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private static final List<String> FLUSH_KEYS = List.of(
        Constants.FLUSH_SUCCESS_MSG_CODE,
        Constants.FLUSH_ERROR_MSG_CODE,
        Constants.FLUSH_WARNING_MSG_CODE,
        Constants.FLUSH_INFO_MSG_CODE
    );

    public void removeFlushMessageFromSession() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            logger.warn("No servlet request bound to current thread. Cannot clear flush messages.");
            return;
        }

        HttpSession session = servletAttrs.getRequest().getSession(false);
        if (Objects.isNull(session)) {
            logger.debug("No active session found. Nothing to clear.");
            return;
        }

        FLUSH_KEYS.forEach(key -> {
            if (Objects.nonNull(session.getAttribute(key))) {
                logger.debug("Removing flush message({}) from session", key);
                session.removeAttribute(key);
            }
        });
    }
}
