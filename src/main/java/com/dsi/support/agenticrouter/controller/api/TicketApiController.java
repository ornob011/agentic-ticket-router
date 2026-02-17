package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.TicketQueryScope;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.service.ticket.TicketAssignmentCommandService;
import com.dsi.support.agenticrouter.service.ticket.TicketLifecycleCommandService;
import com.dsi.support.agenticrouter.service.ticket.TicketQueryService;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketApiController {

    private final TicketLifecycleCommandService ticketLifecycleCommandService;
    private final TicketAssignmentCommandService ticketAssignmentCommandService;
    private final TicketQueryService ticketQueryService;

    @GetMapping
    @PreAuthorize("@ticketAuthorizationService.canAccessQueueScope(#scope,#queue)")
    public ApiDtos.PagedResponse<ApiDtos.TicketSummary> listTickets(
        @RequestParam(defaultValue = "MINE") TicketQueryScope scope,
        @RequestParam(required = false) TicketQueue queue,
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ticketQueryService.listTickets(
            scope,
            queue,
            status,
            page,
            size,
            Utils.getLoggedInUserDetails()
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiDtos.TicketSummary createTicket(
        @Valid @RequestBody ApiDtos.TicketCreateRequest request
    ) {
        AppUser user = Utils.getLoggedInUserDetails();
        CreateTicketDto createTicketDto = CreateTicketDto.builder()
                                                         .subject(request.subject())
                                                         .content(request.content())
                                                         .build();

        SupportTicket createdTicket = ticketLifecycleCommandService.createTicket(
            createTicketDto,
            user.getId()
        );

        return ticketQueryService.toTicketSummary(
            createdTicket
        );
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("@ticketAuthorizationService.canAccessTicket(#ticketId)")
    public ApiDtos.TicketDetail getTicket(
        @PathVariable Long ticketId
    ) {
        return ticketQueryService.getTicketDetail(
            ticketId,
            Utils.getLoggedInUserDetails()
        );
    }

    @GetMapping("/assignable-agents")
    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    public List<ApiDtos.AssignableAgentOption> assignableAgents() {
        return ticketQueryService.assignableAgents();
    }

    @PostMapping("/{ticketId}/replies")
    @PreAuthorize("@ticketAuthorizationService.canAccessTicket(#ticketId)")
    public void addReply(
        @PathVariable Long ticketId,
        @Valid @RequestBody ApiDtos.TicketReplyRequest request
    ) throws BindException {
        AppUser user = Utils.getLoggedInUserDetails();

        if (user.isCustomer()) {
            ticketLifecycleCommandService.addCustomerReply(
                ticketId,
                request.content(),
                user.getId()
            );
        } else {
            ticketLifecycleCommandService.addAgentReply(
                ticketId,
                request.content()
            );
        }
    }

    @PatchMapping("/{ticketId}/status")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISOR','ADMIN')")
    public void changeStatus(
        @PathVariable Long ticketId,
        @Valid @RequestBody ApiDtos.TicketStatusRequest request
    ) throws BindException {
        ticketLifecycleCommandService.changeTicketStatus(
            ticketId,
            request.newStatus(),
            request.reason()
        );
    }

    @PatchMapping("/{ticketId}/assign-agent")
    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    public void assignAgent(
        @PathVariable Long ticketId,
        @Valid @RequestBody ApiDtos.AssignAgentRequest request
    ) throws BindException {
        ticketAssignmentCommandService.assignAgent(
            ticketId,
            request.agentId()
        );
    }

    @PatchMapping("/{ticketId}/assign-self")
    @PreAuthorize("hasRole('AGENT')")
    public void assignSelf(
        @PathVariable Long ticketId
    ) throws BindException {
        ticketAssignmentCommandService.assignSelf(
            ticketId
        );
    }

    @PatchMapping("/{ticketId}/release-agent")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISOR','ADMIN')")
    public void releaseAgent(
        @PathVariable Long ticketId
    ) throws BindException {
        ticketAssignmentCommandService.releaseAgent(
            ticketId
        );
    }

    @PatchMapping("/{ticketId}/override-routing")
    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    public void overrideRouting(
        @PathVariable Long ticketId,
        @Valid @RequestBody ApiDtos.RoutingOverrideRequest request
    ) throws BindException {
        ticketLifecycleCommandService.overrideRouting(
            ticketId,
            request.queue(),
            request.priority(),
            request.reason()
        );
    }

    @GetMapping("/meta")
    public ApiDtos.TicketMetadataResponse metadata() {
        return ticketQueryService.metadata(
            Utils.getLoggedInUserDetails()
        );
    }

}
