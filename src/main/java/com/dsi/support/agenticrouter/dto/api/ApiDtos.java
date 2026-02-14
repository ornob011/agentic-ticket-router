package com.dsi.support.agenticrouter.dto.api;

import com.dsi.support.agenticrouter.enums.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiDtos {

    private ApiDtos() {
        // Private constructor to prevent instantiation
    }

    @Builder
    public record UserMe(
        Long id,
        String username,
        String fullName,
        UserRole role,
        String roleLabel
    ) {
    }

    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        boolean rememberMe
    ) {
    }

    public record SignupRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 72) String password,
        @NotBlank String confirmPassword,
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Size(max = 100) String companyName,
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 100) String city,
        @NotBlank String countryIso2,
        @NotBlank String customerTierCode,
        @NotBlank String preferredLanguageCode
    ) {
    }

    @Builder
    public record LookupOption(
        String code,
        String name
    ) {
    }

    @Builder
    public record SignupOptionsResponse(
        List<LookupOption> countries,
        List<LookupOption> tiers,
        List<LookupOption> languages
    ) {
    }

    @Builder
    public record ValidationFieldError(
        String field,
        String message
    ) {
    }

    @Builder
    public record ValidationErrorResponse(
        List<ValidationFieldError> fieldErrors,
        List<String> globalErrors,
        List<String> errors
    ) {
    }

    @Builder
    public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
    ) {
    }

    @Builder
    public record TicketSummary(
        Long id,
        String formattedTicketNo,
        String subject,
        TicketStatus status,
        String statusLabel,
        TicketCategory category,
        String categoryLabel,
        TicketPriority priority,
        String priorityLabel,
        TicketQueue queue,
        String queueLabel,
        Instant lastActivityAt,
        String customerName,
        String assignedAgentName
    ) {
    }

    @Builder
    public record TicketMessageItem(
        Long id,
        String content,
        String authorName,
        UserRole authorRole,
        String messageKind,
        boolean visibleToCustomer,
        Instant createdAt
    ) {
    }

    @Builder
    public record AuditEventItem(
        Long id,
        AuditEventType eventType,
        String eventTypeLabel,
        String description,
        String performedBy,
        Instant createdAt
    ) {
    }

    @Builder
    public record TicketRoutingItem(
        Long id,
        Integer version,
        TicketCategory category,
        String categoryLabel,
        TicketPriority priority,
        String priorityLabel,
        TicketQueue queue,
        String queueLabel,
        String nextAction,
        String nextActionLabel,
        BigDecimal confidence,
        boolean overridden,
        String overrideReason,
        Instant createdAt
    ) {
    }

    @Builder
    public record TicketDetail(
        Long id,
        String formattedTicketNo,
        String subject,
        TicketStatus status,
        String statusLabel,
        TicketCategory category,
        String categoryLabel,
        TicketPriority priority,
        String priorityLabel,
        TicketQueue queue,
        String queueLabel,
        Instant createdAt,
        Instant updatedAt,
        Instant lastActivityAt,
        int reopenCount,
        boolean escalated,
        UserMe customer,
        UserMe assignedAgent,
        List<TicketMessageItem> messages,
        List<AuditEventItem> auditEvents,
        List<TicketRoutingItem> routingHistory
    ) {
    }

    public record TicketCreateRequest(
        @NotBlank String subject,
        @NotBlank String content
    ) {
    }

    public record TicketReplyRequest(
        @NotBlank String content
    ) {
    }

    public record TicketStatusRequest(
        @NotNull TicketStatus newStatus,
        @Size(max = 500) String reason
    ) {
    }

    public record AssignAgentRequest(
        @NotNull Long agentId
    ) {
    }

    public record RoutingOverrideRequest(
        @NotNull TicketQueue queue,
        @NotNull TicketPriority priority,
        @NotBlank String reason
    ) {
    }

    @Builder
    public record TicketMetadataResponse(
        List<LookupOption> queues,
        List<LookupOption> statuses,
        List<LookupOption> priorities
    ) {
    }

    @Builder
    public record DashboardResponse(
        UserMe user,
        DashboardCustomerSection customer,
        DashboardAgentSection agent,
        DashboardSupervisorSection supervisor,
        DashboardAdminSection admin,
        List<TicketSummary> recentTickets
    ) {
    }

    @Builder
    public record DashboardCustomerSection(
        long openTickets,
        long waitingOnMe,
        long resolvedTickets,
        long closedTickets
    ) {
    }

    @Builder
    public record DashboardAgentSection(
        long myAssignedCount,
        long queueBilling,
        long queueTech,
        long queueOps,
        long queueSecurity,
        long queueAccount,
        long queueGeneral
    ) {
    }

    @Builder
    public record DashboardSupervisorSection(
        long pendingEscalations,
        long slaBreaches,
        long humanReviewCount
    ) {
    }

    @Builder
    public record DashboardAdminSection(
        long totalUsers,
        long totalTickets,
        String activeModelTag,
        double routingSuccessRate,
        Long avgRoutingLatency
    ) {
    }

    @Builder
    public record EscalationSummary(
        Long id,
        Long ticketId,
        String formattedTicketNo,
        String reason,
        boolean resolved,
        Instant createdAt,
        String assignedSupervisor
    ) {
    }

    @Builder
    public record EscalationDetail(
        Long id,
        Long ticketId,
        String formattedTicketNo,
        String reason,
        boolean resolved,
        String resolutionNotes,
        Instant createdAt,
        Instant resolvedAt,
        String assignedSupervisor,
        String resolvedBy
    ) {
    }

    public record ResolveEscalationRequest(
        @NotBlank String resolutionNotes
    ) {
    }

    @Builder
    public record ModelInfo(
        Long id,
        String modelTag,
        String provider,
        boolean active,
        Long activatedBy,
        Instant activatedAt
    ) {
    }

    public record ActivateModelRequest(
        @NotBlank String modelTag
    ) {
    }

    @Builder
    public record PolicyInfo(
        Long id,
        String configKey,
        BigDecimal configValue,
        boolean active
    ) {
    }

    public record PolicyUpdateRequest(
        @NotBlank String configKey,
        @NotNull BigDecimal configValue
    ) {
    }

    @Builder
    public record UserInfo(
        Long id,
        String username,
        String email,
        String fullName,
        UserRole role,
        String roleLabel,
        boolean active
    ) {
    }

    public record StaffCreateRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotNull UserRole role,
        @NotBlank String password
    ) {
    }
}
