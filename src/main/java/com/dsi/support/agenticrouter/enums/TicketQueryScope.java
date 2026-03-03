package com.dsi.support.agenticrouter.enums;

import com.dsi.support.agenticrouter.util.StringNormalizationUtils;

import java.util.Objects;

public enum TicketQueryScope {
    MINE,
    QUEUE,
    REVIEW,
    ALL;

    public static TicketQueryScope from(
        String scope
    ) {
        if (Objects.isNull(scope)) {
            throw new IllegalArgumentException("Unsupported scope: null");
        }

        return TicketQueryScope.valueOf(
            StringNormalizationUtils.upperTrimmedOrEmpty(scope)
        );
    }
}
