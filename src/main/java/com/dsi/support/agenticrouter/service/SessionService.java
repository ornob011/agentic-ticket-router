package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.util.Constants;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class SessionService {

    private static final List<String> FLUSH_KEYS = List.of(
        Constants.FLUSH_SUCCESS_MSG_CODE,
        Constants.FLUSH_ERROR_MSG_CODE,
        Constants.FLUSH_WARNING_MSG_CODE,
        Constants.FLUSH_INFO_MSG_CODE
    );

    public void removeFlushMessageFromSession() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            log.warn(
                "SessionFlushMessageClear(skip) Outcome(reason:{})",
                "no_servlet_request_attributes"
            );

            return;
        }

        HttpSession session = servletAttrs.getRequest().getSession(false);
        if (Objects.isNull(session)) {
            log.debug(
                "SessionFlushMessageClear(skip) Outcome(reason:{})",
                "no_http_session"
            );

            return;
        }

        FLUSH_KEYS.forEach(key -> {
            if (Objects.nonNull(session.getAttribute(key))) {
                log.debug(
                    "SessionFlushMessageClear(persist) HttpSession(id:{}) Outcome(attribute:{})",
                    session.getId(),
                    key
                );

                session.removeAttribute(key);
            }
        });
    }
}
