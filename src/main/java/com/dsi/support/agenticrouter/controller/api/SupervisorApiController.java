package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.enums.EscalationFilterStatus;
import com.dsi.support.agenticrouter.service.ticket.TicketEscalationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supervisor/escalations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
public class SupervisorApiController {

    private final TicketEscalationService ticketEscalationService;

    @GetMapping
    public ApiDtos.PagedResponse<ApiDtos.EscalationSummary> listEscalations(
        @RequestParam(required = false) EscalationFilterStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ticketEscalationService.listEscalationSummaries(
            status,
            page,
            size
        );
    }

    @GetMapping("/{escalationId}")
    public ApiDtos.EscalationDetail getEscalation(
        @PathVariable Long escalationId
    ) {
        return ticketEscalationService.getEscalationDetail(
            escalationId
        );
    }

    @GetMapping("/assignable-supervisors")
    public List<ApiDtos.AssignableSupervisorOption> assignableSupervisors() {
        return ticketEscalationService.listAssignableSupervisors();
    }

    @PatchMapping("/{escalationId}/assign-supervisor")
    public void assignSupervisor(
        @PathVariable Long escalationId,
        @Valid @RequestBody ApiDtos.AssignEscalationSupervisorRequest request
    ) throws BindException {
        ticketEscalationService.assignSupervisor(
            escalationId,
            request.supervisorId()
        );
    }

    @PostMapping("/{escalationId}/resolve")
    public void resolveEscalation(
        @PathVariable Long escalationId,
        @Valid @RequestBody ApiDtos.ResolveEscalationRequest request
    ) throws BindException {
        ticketEscalationService.resolveEscalation(
            escalationId,
            request.resolutionNotes()
        );
    }
}
