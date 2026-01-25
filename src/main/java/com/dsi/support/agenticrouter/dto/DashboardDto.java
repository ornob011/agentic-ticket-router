package com.dsi.support.agenticrouter.dto;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder
public record DashboardDto(
    AppUser user,
    CustomerData customerData,
    AgentData agentData,
    SupervisorData supervisorData,
    AdminData adminData
) {

    @Builder
    public record CustomerData(
        long openTickets,
        long resolvedTickets,
        long closedTickets,
        long waitingOnMe,
        List<TicketSummary> recentTickets
    ) {
    }

    @Builder
    public record AgentData(
        Map<TicketQueue, Long> queueCounts,
        long myAssignedCount,
        List<TicketSummary> myTickets,
        Map<TicketStatus, Long> myTicketStatusCounts
    ) {
    }

    @Builder
    public record SupervisorData(
        long pendingEscalations,
        long slaBreaches,
        long humanReviewCount,
        List<EscalationSummary> recentEscalations
    ) {
    }

    @Builder
    public record AdminData(
        long totalUsers,
        long totalTickets,
        String activeModelTag,
        double routingSuccessRate,
        long recentAuditEvents
    ) {
    }

    @Builder
    public record TicketSummary(
        Long id,
        String ticketNo,
        String subject,
        TicketStatus status,
        Instant lastActivityAt,
        String formattedTicketNo
    ) {
    }

    @Builder
    public record EscalationSummary(
        Long id,
        String ticketNo,
        String reason,
        Instant createdAt,
        boolean resolved
    ) {
    }
}
