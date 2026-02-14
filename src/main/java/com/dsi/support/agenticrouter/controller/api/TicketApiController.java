package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.CreateTicketDto;
import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.TicketPriority;
import com.dsi.support.agenticrouter.enums.TicketQueryScope;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.ticket.TicketService;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketApiController {

    private final TicketService ticketService;
    private final AuditService auditService;
    private final SupportTicketRepository supportTicketRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    @PreAuthorize("@ticketAuthorizationService.canAccessQueueScope(#scope)")
    public ApiDtos.PagedResponse<ApiDtos.TicketSummary> listTickets(
        @RequestParam(defaultValue = "MINE") TicketQueryScope scope,
        @RequestParam(required = false) TicketQueue queue,
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        AppUser user = Utils.getLoggedInUserDetails();

        Pageable pageable = PageRequest.of(
            page,
            size
        );

        Page<SupportTicket> ticketPage = resolveTicketsByScope(
            scope,
            user,
            queue,
            status,
            pageable
        );

        return toPagedTicketSummary(ticketPage);
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

        ticketService.createTicket(
            createTicketDto,
            user.getId()
        );

        SupportTicket createdTicket = supportTicketRepository.findByCustomerIdOrderByCreatedAtDesc(
                                                                 user.getId(),
                                                                 PageRequest.of(
                                                                     0,
                                                                     1
                                                                 )
                                                             )
                                                             .stream()
                                                             .findFirst()
                                                             .orElseThrow();

        return toTicketSummary(createdTicket);
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("@ticketAuthorizationService.canAccessTicket(#ticketId)")
    public ApiDtos.TicketDetail getTicket(
        @PathVariable Long ticketId
    ) {
        SupportTicket supportTicket = ticketService.getTicketDetail(ticketId);

        List<TicketMessage> ticketMessages = ticketService.getTicketMessages(ticketId);
        List<TicketRouting> routingHistory = ticketService.getTicketRoutingHistory(ticketId);

        AppUser user = Utils.getLoggedInUserDetails();
        if (user.isCustomer()) {
            ticketMessages = ticketMessages.stream()
                                           .filter(TicketMessage::isVisibleToCustomer)
                                           .toList();
        }

        List<ApiDtos.TicketMessageItem> ticketMessageItems = ticketMessages.stream()
                                                                           .map(this::toMessageItem)
                                                                           .toList();

        List<ApiDtos.AuditEventItem> auditEventItems = auditService.getTicketAuditTrailView(ticketId)
                                                                   .stream()
                                                                   .map(auditEventView -> ApiDtos.AuditEventItem.builder()
                                                                                                                .id(auditEventView.getId())
                                                                                                                .eventType(auditEventView.getEventType())
                                                                                                                .eventTypeLabel(EnumDisplayNameResolver.resolve(
                                                                                                                    auditEventView.getEventType()
                                                                                                                ))
                                                                                                                .description(auditEventView.getDescription())
                                                                                                                .performedBy(auditEventView.getPerformedByName())
                                                                                                                .createdAt(auditEventView.getCreatedAt())
                                                                                                                .build())
                                                                   .toList();

        List<ApiDtos.TicketRoutingItem> ticketRoutingItems = new ArrayList<>();
        for (TicketRouting ticketRouting : routingHistory) {
            ApiDtos.TicketRoutingItem ticketRoutingItem = ApiDtos.TicketRoutingItem.builder()
                                                                                   .id(ticketRouting.getId())
                                                                                   .version(ticketRouting.getVersion())
                                                                                   .category(ticketRouting.getCategory())
                                                                                   .categoryLabel(EnumDisplayNameResolver.resolve(
                                                                                       ticketRouting.getCategory()
                                                                                   ))
                                                                                   .priority(ticketRouting.getPriority())
                                                                                   .priorityLabel(EnumDisplayNameResolver.resolve(
                                                                                       ticketRouting.getPriority()
                                                                                   ))
                                                                                   .queue(ticketRouting.getQueue())
                                                                                   .queueLabel(EnumDisplayNameResolver.resolve(
                                                                                       ticketRouting.getQueue()
                                                                                   ))
                                                                                   .nextAction(ticketRouting.getNextAction().name())
                                                                                   .nextActionLabel(EnumDisplayNameResolver.resolve(
                                                                                       ticketRouting.getNextAction()
                                                                                   ))
                                                                                   .confidence(ticketRouting.getConfidence())
                                                                                   .overridden(ticketRouting.isOverridden())
                                                                                   .overrideReason(ticketRouting.getOverrideReason())
                                                                                   .createdAt(ticketRouting.getCreatedAt())
                                                                                   .build();
            ticketRoutingItems.add(ticketRoutingItem);
        }

        ApiDtos.UserMe customer = toUserMe(supportTicket.getCustomer());
        ApiDtos.UserMe assignedAgent = Objects.isNull(supportTicket.getAssignedAgent())
            ? null
            : toUserMe(supportTicket.getAssignedAgent());

        return ApiDtos.TicketDetail.builder()
                                   .id(supportTicket.getId())
                                   .formattedTicketNo(supportTicket.getFormattedTicketNo())
                                   .subject(supportTicket.getSubject())
                                   .status(supportTicket.getStatus())
                                   .statusLabel(EnumDisplayNameResolver.resolve(
                                       supportTicket.getStatus()
                                   ))
                                   .category(supportTicket.getCurrentCategory())
                                   .categoryLabel(EnumDisplayNameResolver.resolve(
                                       supportTicket.getCurrentCategory()
                                   ))
                                   .priority(supportTicket.getCurrentPriority())
                                   .priorityLabel(EnumDisplayNameResolver.resolve(
                                       supportTicket.getCurrentPriority()
                                   ))
                                   .queue(supportTicket.getAssignedQueue())
                                   .queueLabel(EnumDisplayNameResolver.resolve(
                                       supportTicket.getAssignedQueue()
                                   ))
                                   .createdAt(supportTicket.getCreatedAt())
                                   .updatedAt(supportTicket.getUpdatedAt())
                                   .lastActivityAt(supportTicket.getLastActivityAt())
                                   .reopenCount(supportTicket.getReopenCount())
                                   .escalated(supportTicket.isEscalated())
                                   .customer(customer)
                                   .assignedAgent(assignedAgent)
                                   .messages(ticketMessageItems)
                                   .auditEvents(auditEventItems)
                                   .routingHistory(ticketRoutingItems)
                                   .build();
    }

    @PostMapping("/{ticketId}/replies")
    @PreAuthorize("@ticketAuthorizationService.canAccessTicket(#ticketId)")
    public void addReply(
        @PathVariable Long ticketId,
        @Valid @RequestBody ApiDtos.TicketReplyRequest request
    ) {
        AppUser user = Utils.getLoggedInUserDetails();
        if (user.isCustomer()) {
            ticketService.addCustomerReply(
                ticketId,
                request.content(),
                user.getId()
            );
        } else {
            ticketService.addAgentReply(
                ticketId,
                request.content()
            );
        }
    }

    @PatchMapping("/{ticketId}/status")
    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    public void changeStatus(
        @PathVariable Long ticketId,
        @Valid @RequestBody ApiDtos.TicketStatusRequest request
    ) {
        ticketService.changeTicketStatus(
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
    ) {
        ticketService.assignAgent(
            ticketId,
            request.agentId()
        );
    }

    @PatchMapping("/{ticketId}/release-agent")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISOR','ADMIN')")
    public void releaseAgent(
        @PathVariable Long ticketId
    ) {
        ticketService.releaseAgent(
            ticketId
        );
    }

    @PatchMapping("/{ticketId}/override-routing")
    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    public void overrideRouting(
        @PathVariable Long ticketId,
        @Valid @RequestBody ApiDtos.RoutingOverrideRequest request
    ) {
        ticketService.overrideRouting(
            ticketId,
            request.queue(),
            request.priority(),
            request.reason()
        );
    }

    @GetMapping("/meta")
    public ApiDtos.TicketMetadataResponse metadata() {
        List<ApiDtos.LookupOption> queueOptions = java.util.Arrays.stream(TicketQueue.values())
                                                                  .map(ticketQueue -> ApiDtos.LookupOption.builder()
                                                                                                          .code(ticketQueue.name())
                                                                                                          .name(EnumDisplayNameResolver.resolve(ticketQueue))
                                                                                                          .build())
                                                                  .toList();

        List<ApiDtos.LookupOption> statusOptions = java.util.Arrays.stream(TicketStatus.values())
                                                                   .map(ticketStatus -> ApiDtos.LookupOption.builder()
                                                                                                            .code(ticketStatus.name())
                                                                                                            .name(EnumDisplayNameResolver.resolve(ticketStatus))
                                                                                                            .build())
                                                                   .toList();

        List<ApiDtos.LookupOption> priorityOptions = java.util.Arrays.stream(TicketPriority.values())
                                                                     .map(ticketPriority -> ApiDtos.LookupOption.builder()
                                                                                                                .code(ticketPriority.name())
                                                                                                                .name(EnumDisplayNameResolver.resolve(ticketPriority))
                                                                                                                .build())
                                                                     .toList();

        return ApiDtos.TicketMetadataResponse.builder()
                                             .queues(queueOptions)
                                             .statuses(statusOptions)
                                             .priorities(priorityOptions)
                                             .build();
    }

    private Page<SupportTicket> resolveTicketsByScope(
        TicketQueryScope scope,
        AppUser user,
        TicketQueue queue,
        TicketStatus status,
        Pageable pageable
    ) {
        return switch (scope) {
            case MINE -> user.isCustomer()
                ? ticketService.listCustomerTickets(
                user.getId(),
                pageable
            )
                : supportTicketRepository.findByAssignedAgentIdOrderByLastActivityAtDesc(
                user.getId(),
                pageable
            );
            case QUEUE -> ticketService.listQueueTickets(
                queue,
                status,
                pageable
            );
            case REVIEW -> supportTicketRepository.findByStatusAndAssignedQueueIsNull(
                TicketStatus.TRIAGING,
                pageable
            );
            case ALL -> supportTicketRepository.findAll(pageable);
        };
    }

    private ApiDtos.PagedResponse<ApiDtos.TicketSummary> toPagedTicketSummary(
        Page<SupportTicket> ticketPage
    ) {
        List<ApiDtos.TicketSummary> content = ticketPage.getContent()
                                                        .stream()
                                                        .map(this::toTicketSummary)
                                                        .toList();
        ApiDtos.PagedResponse<ApiDtos.TicketSummary> pagedResponse = ApiDtos.PagedResponse.<ApiDtos.TicketSummary>builder()
                                                                                          .content(content)
                                                                                          .page(ticketPage.getNumber())
                                                                                          .size(ticketPage.getSize())
                                                                                          .totalElements(ticketPage.getTotalElements())
                                                                                          .totalPages(ticketPage.getTotalPages())
                                                                                          .hasNext(ticketPage.hasNext())
                                                                                          .build();
        return pagedResponse;
    }

    private ApiDtos.TicketSummary toTicketSummary(SupportTicket supportTicket) {
        return ApiDtos.TicketSummary.builder()
                                    .id(supportTicket.getId())
                                    .formattedTicketNo(supportTicket.getFormattedTicketNo())
                                    .subject(supportTicket.getSubject())
                                    .status(supportTicket.getStatus())
                                    .statusLabel(EnumDisplayNameResolver.resolve(
                                        supportTicket.getStatus()
                                    ))
                                    .category(supportTicket.getCurrentCategory())
                                    .categoryLabel(EnumDisplayNameResolver.resolve(
                                        supportTicket.getCurrentCategory()
                                    ))
                                    .priority(supportTicket.getCurrentPriority())
                                    .priorityLabel(EnumDisplayNameResolver.resolve(
                                        supportTicket.getCurrentPriority()
                                    ))
                                    .queue(supportTicket.getAssignedQueue())
                                    .queueLabel(EnumDisplayNameResolver.resolve(
                                        supportTicket.getAssignedQueue()
                                    ))
                                    .lastActivityAt(supportTicket.getLastActivityAt())
                                    .customerName(null)
                                    .assignedAgentName(null)
                                    .build();
    }

    private ApiDtos.UserMe toUserMe(AppUser appUser) {
        return ApiDtos.UserMe.builder()
                             .id(appUser.getId())
                             .username(appUser.getUsername())
                             .fullName(appUser.getFullName())
                             .role(appUser.getRole())
                             .roleLabel(EnumDisplayNameResolver.resolve(
                                 appUser.getRole()
                             ))
                             .build();
    }

    private ApiDtos.TicketMessageItem toMessageItem(TicketMessage ticketMessage) {
        AppUser author = ticketMessage.getAuthor();
        return ApiDtos.TicketMessageItem.builder()
                                        .id(ticketMessage.getId())
                                        .content(ticketMessage.getContent())
                                        .authorName(Objects.isNull(author) ? "SYSTEM" : author.getFullName())
                                        .authorRole(Objects.isNull(author) ? null : author.getRole())
                                        .messageKind(ticketMessage.getMessageKind().name())
                                        .visibleToCustomer(ticketMessage.isVisibleToCustomer())
                                        .createdAt(ticketMessage.getCreatedAt())
                                        .build();
    }

}
