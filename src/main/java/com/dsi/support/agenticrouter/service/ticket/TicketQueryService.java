package com.dsi.support.agenticrouter.service.ticket;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.entity.TicketMessage;
import com.dsi.support.agenticrouter.entity.TicketRouting;
import com.dsi.support.agenticrouter.enums.TicketQueryScope;
import com.dsi.support.agenticrouter.enums.TicketQueue;
import com.dsi.support.agenticrouter.enums.TicketStatus;
import com.dsi.support.agenticrouter.enums.UserRole;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.repository.TicketMessageRepository;
import com.dsi.support.agenticrouter.repository.TicketRoutingRepository;
import com.dsi.support.agenticrouter.security.TicketAccessPolicyService;
import com.dsi.support.agenticrouter.service.audit.AuditService;
import com.dsi.support.agenticrouter.service.common.AuditEventItemMapper;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.PageResponseUtils;
import com.dsi.support.agenticrouter.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TicketQueryService {

    private final SupportTicketRepository supportTicketRepository;
    private final AppUserRepository appUserRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketRoutingRepository ticketRoutingRepository;
    private final AuditService auditService;
    private final TicketAccessPolicyService ticketAccessPolicyService;
    private final TicketPermissionService ticketPermissionService;
    private final TicketMetadataService ticketMetadataService;
    private final TicketDtoMapper ticketDtoMapper;
    private final AuditEventItemMapper auditEventItemMapper;

    @Transactional(readOnly = true)
    public ApiDtos.PagedResponse<ApiDtos.TicketSummary> listTickets(
        TicketQueryScope scope,
        TicketQueue queue,
        TicketStatus status,
        int page,
        int size,
        AppUser user
    ) {
        Pageable pageable = PaginationUtils.normalize(
            page,
            size
        );

        TicketQueryScope effectiveScope = Objects.requireNonNullElse(
            scope,
            TicketQueryScope.MINE
        );

        if (!ticketAccessPolicyService.canAccessQueueScope(
            effectiveScope,
            queue,
            user
        )) {
            throw new AccessDeniedException(
                "Not authorized to access tickets for the requested scope."
            );
        }

        Page<SupportTicket> ticketPage = resolveTicketsByScope(
            effectiveScope,
            user,
            queue,
            status,
            pageable
        );

        return PageResponseUtils.fromPage(
            ticketPage,
            ticketDtoMapper::toTicketSummary
        );
    }

    @Transactional(readOnly = true)
    public ApiDtos.TicketDetail getTicketDetail(
        Long ticketId,
        AppUser user
    ) {
        SupportTicket supportTicket = supportTicketRepository.findTicketDetailById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        if (!ticketAccessPolicyService.canAccessTicket(
            supportTicket,
            user
        )) {
            throw new DataNotFoundException(
                SupportTicket.class,
                ticketId
            );
        }

        List<TicketMessage> ticketMessages = ticketMessageRepository.findByTicketIdWithAuthorOrderByCreatedAtAsc(
            ticketId
        );

        List<TicketRouting> routingHistory = ticketRoutingRepository.findByTicketIdOrderByCreatedAtDesc(
            ticketId
        );

        if (user.isCustomer()) {
            ticketMessages = ticketMessages.stream()
                                           .filter(TicketMessage::isVisibleToCustomer)
                                           .toList();
        }

        List<ApiDtos.TicketMessageItem> ticketMessageItems = ticketMessages.stream()
                                                                           .map(ticketDtoMapper::toMessageItem)
                                                                           .toList();

        List<ApiDtos.AuditEventItem> auditEventItems = auditService.getTicketAuditTrailView(ticketId)
                                                                   .stream()
                                                                   .map(auditEventItemMapper::toAuditEventItem)
                                                                   .toList();

        List<ApiDtos.TicketRoutingItem> ticketRoutingItems = ticketDtoMapper.toRoutingItems(
            routingHistory
        );

        ApiDtos.UserMe customer = ticketDtoMapper.toUserMe(supportTicket.getCustomer());
        ApiDtos.UserMe assignedAgent = Objects.isNull(supportTicket.getAssignedAgent())
            ? null
            : ticketDtoMapper.toUserMe(supportTicket.getAssignedAgent());

        ApiDtos.TicketPermissions permissions = ticketPermissionService.resolvePermissions(
            supportTicket,
            user
        );

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

    @Transactional(readOnly = true)
    public ApiDtos.TicketMetadataResponse metadata(
        AppUser user
    ) {
        return ticketMetadataService.metadata(
            user
        );
    }

    public ApiDtos.TicketSummary toTicketSummary(
        SupportTicket supportTicket
    ) {
        return ticketDtoMapper.toTicketSummary(
            supportTicket
        );
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.AssignableAgentOption> assignableAgents() {
        return appUserRepository.findByRoleAndActiveTrue(UserRole.AGENT)
                                .stream()
                                .sorted(
                                    Comparator.comparing(
                                                  AppUser::getFullName,
                                                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                                              )
                                              .thenComparing(
                                                  AppUser::getUsername,
                                                  String.CASE_INSENSITIVE_ORDER
                                              )
                                )
                                .map(appUser -> {
                                    long openTickets = supportTicketRepository.countOpenTicketsByAgentId(appUser.getId());
                                    return ApiDtos.AssignableAgentOption.builder()
                                                                        .id(appUser.getId())
                                                                        .fullName(appUser.getFullName())
                                                                        .username(appUser.getUsername())
                                                                        .openTickets(openTickets)
                                                                        .build();
                                })
                                .toList();
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

}
