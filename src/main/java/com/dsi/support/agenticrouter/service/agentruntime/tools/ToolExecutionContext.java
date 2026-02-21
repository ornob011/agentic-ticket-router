package com.dsi.support.agenticrouter.service.agentruntime.tools;

import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueue;

import java.util.Objects;

public final class ToolExecutionContext {

    private static final ThreadLocal<ToolExecutionContextState> CONTEXT = new ThreadLocal<>();

    private ToolExecutionContext() {
    }

    public static void init(
        SupportTicket ticket
    ) {
        CONTEXT.set(
            new ToolExecutionContextState(
                ticket
            )
        );
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static SupportTicket currentTicket() {
        ToolExecutionContextState state = CONTEXT.get();

        if (Objects.isNull(state)) {
            throw new IllegalStateException(
                "ToolExecutionContext not initialized"
            );
        }

        return state.ticket();
    }

    public static Long currentTicketId() {
        return currentTicket().getId();
    }

    public static Long currentCustomerId() {
        return currentTicket().getCustomer().getId();
    }

    public static Long currentAgentId() {
        if (Objects.isNull(currentTicket().getAssignedAgent())) {
            return null;
        }

        return currentTicket().getAssignedAgent().getId();
    }

    public static TicketCategory currentCategory() {
        return currentTicket().getCurrentCategory();
    }

    public static TicketPriority currentPriority() {
        return currentTicket().getCurrentPriority();
    }

    public static TicketQueue currentQueue() {
        return currentTicket().getAssignedQueue();
    }

    private record ToolExecutionContextState(
        SupportTicket ticket
    ) {
    }
}
