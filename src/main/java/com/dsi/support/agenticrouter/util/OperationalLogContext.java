package com.dsi.support.agenticrouter.util;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;

import java.util.Objects;

public final class OperationalLogContext {

    public static final String PHASE_START = "start";
    public static final String PHASE_DECISION = "decision";
    public static final String PHASE_PERSIST = "persist";
    public static final String PHASE_EVENT = "event";
    public static final String PHASE_COMPLETE = "complete";
    public static final String PHASE_SKIP = "skip";
    public static final String PHASE_FAIL = "fail";

    private OperationalLogContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Long ticketId(
        SupportTicket supportTicket
    ) {
        return Objects.nonNull(supportTicket) ? supportTicket.getId() : null;
    }

    public static String ticketNo(
        SupportTicket supportTicket
    ) {
        return Objects.nonNull(supportTicket) ? supportTicket.getFormattedTicketNo() : "UNKNOWN";
    }

    public static TicketStatus status(
        SupportTicket supportTicket
    ) {
        return Objects.nonNull(supportTicket) ? supportTicket.getStatus() : null;
    }

    public static TicketQueue queue(
        SupportTicket supportTicket
    ) {
        return Objects.nonNull(supportTicket) ? supportTicket.getAssignedQueue() : null;
    }

    public static TicketPriority priority(
        SupportTicket supportTicket
    ) {
        return Objects.nonNull(supportTicket) ? supportTicket.getCurrentPriority() : null;
    }

    public static Long actorId(
        AppUser appUser
    ) {
        return Objects.nonNull(appUser) ? appUser.getId() : null;
    }

    public static String actorRole(
        AppUser appUser
    ) {
        return Objects.nonNull(appUser) && Objects.nonNull(appUser.getRole())
            ? appUser.getRole().name()
            : null;
    }
}
