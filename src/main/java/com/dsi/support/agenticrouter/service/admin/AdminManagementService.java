package com.dsi.support.agenticrouter.service.admin;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.entity.AgentQueueMembership;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.enums.AuditEventType;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AgentQueueMembershipRepository;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.AuditEventRepository;
import com.dsi.support.agenticrouter.service.common.AuditEventItemMapper;
import com.dsi.support.agenticrouter.util.EnumDisplayNameResolver;
import com.dsi.support.agenticrouter.util.PageResponseUtils;
import com.dsi.support.agenticrouter.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AuditEventRepository auditEventRepository;
    private final AgentQueueMembershipRepository agentQueueMembershipRepository;
    private final AppUserRepository appUserRepository;
    private final AuditEventItemMapper auditEventItemMapper;

    @Transactional(readOnly = true)
    public List<ApiDtos.QueueMembershipInfo> queueMemberships() {
        return agentQueueMembershipRepository.findAllWithUser()
                                             .stream()
                                             .map(membership -> ApiDtos.QueueMembershipInfo.builder()
                                                                                           .id(membership.getId())
                                                                                           .userId(membership.getUser().getId())
                                                                                           .username(membership.getUser().getUsername())
                                                                                           .queue(membership.getQueue())
                                                                                           .queueLabel(EnumDisplayNameResolver.resolve(membership.getQueue()))
                                                                                           .build())
                                             .toList();
    }

    @Transactional
    public void createQueueMembership(
        ApiDtos.QueueMembershipCreateRequest request
    ) {
        AppUser user = appUserRepository.findById(request.userId())
                                        .orElseThrow(
                                            DataNotFoundException.supplier(
                                                AppUser.class,
                                                request.userId()
                                            )
                                        );

        if (!user.canAccessAgentPortal()) {
            throw new IllegalArgumentException(
                "Queue membership can only be assigned to staff users."
            );
        }

        if (agentQueueMembershipRepository.existsByUserIdAndQueue(
            request.userId(),
            request.queue()
        )) {
            return;
        }

        AgentQueueMembership membership = AgentQueueMembership.builder()
                                                              .user(user)
                                                              .queue(request.queue())
                                                              .build();
        agentQueueMembershipRepository.save(membership);
    }

    @Transactional
    public void deleteQueueMembership(
        Long membershipId
    ) {
        AgentQueueMembership membership = agentQueueMembershipRepository.findById(membershipId)
                                                                        .orElseThrow(
                                                                            DataNotFoundException.supplier(
                                                                                AgentQueueMembership.class,
                                                                                membershipId
                                                                            )
                                                                        );

        agentQueueMembershipRepository.delete(
            membership
        );
    }

    @Transactional(readOnly = true)
    public ApiDtos.PagedResponse<ApiDtos.AuditEventItem> auditLog(
        Long ticketId,
        AuditEventType eventType,
        Long performedById,
        Instant startDate,
        Instant endDate,
        int page,
        int size
    ) {
        Pageable pageable = PaginationUtils.normalize(
            page,
            size,
            Sort.by("createdAt").descending()
        );

        Page<AuditEventRepository.AuditEventView> auditEvents = auditEventRepository.findByFiltersView(
            ticketId,
            eventType,
            performedById,
            startDate,
            endDate,
            pageable
        );

        return PageResponseUtils.fromPage(
            auditEvents,
            auditEventItemMapper::toAuditEventItem
        );
    }
}
