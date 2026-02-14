package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.Escalation;
import com.dsi.support.agenticrouter.enums.EscalationFilterStatus;
import com.dsi.support.agenticrouter.service.ticket.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/supervisor/escalations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
public class SupervisorApiController {

    private final TicketService ticketService;

    @GetMapping
    public ApiDtos.PagedResponse<ApiDtos.EscalationSummary> listEscalations(
        @RequestParam(required = false) EscalationFilterStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<Escalation> escalationPage = loadEscalations(
            status,
            PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
            )
        );

        return new ApiDtos.PagedResponse<>(
            escalationPage.map(this::toEscalationSummary).getContent(),
            escalationPage.getNumber(),
            escalationPage.getSize(),
            escalationPage.getTotalElements(),
            escalationPage.getTotalPages(),
            escalationPage.hasNext()
        );
    }

    @GetMapping("/{escalationId}")
    public ApiDtos.EscalationDetail getEscalation(
        @PathVariable Long escalationId
    ) {
        Escalation escalation = ticketService.getEscalationById(escalationId);

        return ApiDtos.EscalationDetail.builder()
                                       .id(escalation.getId())
                                       .ticketId(escalation.getTicket().getId())
                                       .formattedTicketNo(escalation.getTicket().getFormattedTicketNo())
                                       .reason(escalation.getReason())
                                       .resolved(escalation.isResolved())
                                       .resolutionNotes(escalation.getResolutionNotes())
                                       .createdAt(escalation.getCreatedAt())
                                       .resolvedAt(escalation.getResolvedAt())
                                       .assignedSupervisor(
                                           Objects.isNull(escalation.getAssignedSupervisor())
                                               ? null
                                               : escalation.getAssignedSupervisor().getFullName()
                                       )
                                       .resolvedBy(
                                           Objects.isNull(escalation.getResolvedBy())
                                               ? null
                                               : escalation.getResolvedBy().getFullName()
                                       )
                                       .build();
    }

    @PostMapping("/{escalationId}/resolve")
    public void resolveEscalation(
        @PathVariable Long escalationId,
        @Valid @RequestBody ApiDtos.ResolveEscalationRequest request
    ) {
        ticketService.resolveEscalation(
            escalationId,
            request.resolutionNotes()
        );
    }

    private ApiDtos.EscalationSummary toEscalationSummary(Escalation escalation) {
        return ApiDtos.EscalationSummary.builder()
                                        .id(escalation.getId())
                                        .ticketId(escalation.getTicket().getId())
                                        .formattedTicketNo(escalation.getTicket().getFormattedTicketNo())
                                        .reason(escalation.getReason())
                                        .resolved(escalation.isResolved())
                                        .createdAt(escalation.getCreatedAt())
                                        .assignedSupervisor(
                                            Objects.isNull(escalation.getAssignedSupervisor())
                                                ? null
                                                : escalation.getAssignedSupervisor().getFullName()
                                        )
                                        .build();
    }

    private Page<Escalation> loadEscalations(
        EscalationFilterStatus escalationFilterStatus,
        Pageable pageable
    ) {
        if (escalationFilterStatus == EscalationFilterStatus.RESOLVED) {
            return ticketService.listEscalationsByResolved(
                true,
                pageable
            );
        }

        if (escalationFilterStatus == EscalationFilterStatus.PENDING) {
            return ticketService.listEscalationsByResolved(
                false,
                pageable
            );
        }

        return ticketService.listAllEscalations(
            pageable
        );
    }
}
