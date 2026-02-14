package com.dsi.support.agenticrouter.enums;

import java.util.Locale;
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
            scope.trim()
                 .toUpperCase(Locale.ROOT)
        );
    }
}
