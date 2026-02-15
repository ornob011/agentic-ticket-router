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
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.repository.TicketRoutingRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.ticket.TicketAssignmentCommandService;
import com.dsi.support.agenticrouter.service.ticket.TicketLifecycleCommandService;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketApiController {

    private final TicketLifecycleCommandService ticketLifecycleCommandService;
    private final TicketAssignmentCommandService ticketAssignmentCommandService;
    private final AuditService auditService;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketRoutingRepository ticketRoutingRepository;
    private final TicketAccessPolicyService ticketAccessPolicyService;

    @GetMapping
    @PreAuthorize("@ticketAuthorizationService.canAccessQueueScope(#scope,#queue)")
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

        SupportTicket createdTicket = ticketLifecycleCommandService.createTicket(
            createTicketDto,
            user.getId()
        );

        return toTicketSummary(createdTicket);
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("@ticketAuthorizationService.canAccessTicket(#ticketId)")
    public ApiDtos.TicketDetail getTicket(
        @PathVariable Long ticketId
    ) {
        SupportTicket supportTicket = supportTicketRepository.findTicketDetailById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        List<TicketMessage> ticketMessages = ticketMessageRepository.findByTicketIdWithAuthorOrderByCreatedAtAsc(
            ticketId
        );

        List<TicketRouting> routingHistory = ticketRoutingRepository.findByTicketIdOrderByCreatedAtDesc(
            ticketId
        );

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

        ApiDtos.TicketPermissions permissions = ApiDtos.TicketPermissions.builder()
                                                                         .canReply(ticketAccessPolicyService.canReply(supportTicket, user))
                                                                         .canChangeStatus(!ticketAccessPolicyService.allowedStatusTransitions(
                                                                             supportTicket,
                                                                             user
                                                                         ).isEmpty())
                                                                         .canAssignSelf(ticketAccessPolicyService.canAssignSelf(
                                                                             supportTicket,
                                                                             user
                                                                         ))
                                                                         .canAssignOthers(ticketAccessPolicyService.canAssignOthers(
                                                                             user
                                                                         ))
                                                                         .canOverrideRouting(ticketAccessPolicyService.canOverrideRouting(
                                                                             user
                                                                         ))
                                                                         .canResolveEscalation(ticketAccessPolicyService.canResolveEscalation(
                                                                             user
                                                                         ))
                                                                         .allowedStatusTransitions(
                                                                             ticketAccessPolicyService.allowedStatusTransitions(
                                                                                 supportTicket,
                                                                                 user
                                                                             ).stream().toList()
                                                                         )
                                                                         .build();

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
                                   .requiresHumanReview(supportTicket.isRequiresHumanReview())
                                   .permissions(permissions)
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
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISOR','ADMIN')")
    public void assignSelf(
        @PathVariable Long ticketId
    ) {
        ticketAssignmentCommandService.assignSelf(
            ticketId
        );
    }

    @PatchMapping("/{ticketId}/release-agent")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISOR','ADMIN')")
    public void releaseAgent(
        @PathVariable Long ticketId
    ) {
        ticketAssignmentCommandService.releaseAgent(
            ticketId
        );
    }

    @PatchMapping("/{ticketId}/override-routing")
    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    public void overrideRouting(
        @PathVariable Long ticketId,
        @Valid @RequestBody ApiDtos.RoutingOverrideRequest request
    ) {
        ticketLifecycleCommandService.overrideRouting(
            ticketId,
            request.queue(),
            request.priority(),
            request.reason()
        );
    }

    @GetMapping("/meta")
    public ApiDtos.TicketMetadataResponse metadata() {
        AppUser user = Utils.getLoggedInUserDetails();

        List<ApiDtos.LookupOption> queueOptions = Arrays.stream(TicketQueue.values())
                                                        .map(ticketQueue -> ApiDtos.LookupOption.builder()
                                                                                                .code(ticketQueue.name())
                                                                                                .name(EnumDisplayNameResolver.resolve(ticketQueue))
                                                                                                .build())
                                                        .toList();

        List<ApiDtos.LookupOption> accessibleQueueOptions = ticketAccessPolicyService.accessibleQueues(user)
                                                                                     .stream()
                                                                                     .map(ticketQueue -> ApiDtos.LookupOption.builder()
                                                                                                                             .code(ticketQueue.name())
                                                                                                                             .name(EnumDisplayNameResolver.resolve(ticketQueue))
                                                                                                                             .build())
                                                                                     .toList();

        List<ApiDtos.LookupOption> statusOptions = Arrays.stream(TicketStatus.values())
                                                         .map(ticketStatus -> ApiDtos.LookupOption.builder()
                                                                                                  .code(ticketStatus.name())
                                                                                                  .name(EnumDisplayNameResolver.resolve(ticketStatus))
                                                                                                  .build())
                                                         .toList();

        List<ApiDtos.LookupOption> priorityOptions = Arrays.stream(TicketPriority.values())
                                                           .map(ticketPriority -> ApiDtos.LookupOption.builder()
                                                                                                      .code(ticketPriority.name())
                                                                                                      .name(EnumDisplayNameResolver.resolve(ticketPriority))
                                                                                                      .build())
                                                           .toList();

        return ApiDtos.TicketMetadataResponse.builder()
                                             .queues(queueOptions)
                                             .accessibleQueues(accessibleQueueOptions)
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
            case MINE -> resolveMineTickets(
                user,
                status,
                pageable
            );
            case QUEUE -> supportTicketRepository.findQueueTickets(
                queue,
                status,
                TicketStatus.queueInboxDefaults(),
                pageable
            );
            case REVIEW -> supportTicketRepository.findByRequiresHumanReviewTrueAndStatusOrderByLastActivityAtDesc(
                TicketStatus.TRIAGING,
                pageable
            );
            case ALL -> supportTicketRepository.findAll(pageable);
        };
    }

    private Page<SupportTicket> resolveMineTickets(
        AppUser user,
        TicketStatus status,
        Pageable pageable
    ) {
        Long userId = user.getId();
        boolean hasStatusFilter = Objects.nonNull(status);

        if (user.isCustomer()) {
            if (!hasStatusFilter) {
                return supportTicketRepository.findByCustomerIdOrderByCreatedAtDesc(
                    userId,
                    pageable
                );
            }

            return supportTicketRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
                userId,
                status,
                pageable
            );
        }

        if (!hasStatusFilter) {
            return supportTicketRepository.findByAssignedAgentIdOrderByLastActivityAtDesc(
                userId,
                pageable
            );
        }

        return supportTicketRepository.findByAssignedAgentIdAndStatusOrderByLastActivityAtDesc(
            userId,
            status,
            pageable
        );
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
                             .email(appUser.getEmail())
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
