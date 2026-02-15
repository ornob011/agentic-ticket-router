package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.enums.EscalationFilterStatus;
import com.dsi.support.agenticrouter.service.ticket.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

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
        return ticketService.listEscalationSummaries(
            status,
            PageRequest.of(
                page,
                size,
                Sort.by("createdAt")
                    .descending()
            )
        );
    }

    @GetMapping("/{escalationId}")
    public ApiDtos.EscalationDetail getEscalation(
        @PathVariable Long escalationId
    ) {
        return ticketService.getEscalationDetail(
            escalationId
        );
    }

    @PostMapping("/{escalationId}/resolve")
    public void resolveEscalation(
        @PathVariable Long escalationId,
        @Valid @RequestBody ApiDtos.ResolveEscalationRequest request
    ) throws BindException {
        ticketService.resolveEscalation(
            escalationId,
            request.resolutionNotes()
        );
    }
}
