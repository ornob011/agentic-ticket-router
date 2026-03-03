package com.dsi.support.agenticrouter.util;

import com.dsi.support.agenticrouter.enums.*;

import java.util.Objects;

public final class EnumDisplayNameResolver {

    private EnumDisplayNameResolver() {
    }

    public static String resolve(
        TicketStatus ticketStatus
    ) {
        if (Objects.isNull(ticketStatus)) {
            return null;
        }

        return ticketStatus.getDisplayName();
    }

    public static String resolve(
        TicketPriority ticketPriority
    ) {
        if (Objects.isNull(ticketPriority)) {
            return null;
        }

        return ticketPriority.getDisplayName();
    }

    public static String resolve(
        TicketQueue ticketQueue
    ) {
        if (Objects.isNull(ticketQueue)) {
            return null;
        }

        return ticketQueue.getDisplayName();
    }

    public static String resolve(
        TicketCategory ticketCategory
    ) {
        if (Objects.isNull(ticketCategory)) {
            return null;
        }

        return ticketCategory.getDescription();
    }

    public static String resolve(
        AuditEventType auditEventType
    ) {
        if (Objects.isNull(auditEventType)) {
            return null;
        }

        return auditEventType.getDescription();
    }

    public static String resolve(
        NextAction nextAction
    ) {
        if (Objects.isNull(nextAction)) {
            return null;
        }

        return nextAction.getDescription();
    }

    public static String resolve(
        UserRole userRole
    ) {
        if (Objects.isNull(userRole)) {
            return null;
        }

        return userRole.getDisplayName();
    }
}
