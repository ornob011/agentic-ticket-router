package com.dsi.support.agenticrouter.service.dashboard;

import com.dsi.support.agenticrouter.dto.NotificationDto;
import com.dsi.support.agenticrouter.dto.QueueStatsDto;
import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Notification;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final AppUserRepository appUserRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final EscalationRepository escalationRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public QueueStatsDto getQueueStats(
        Long requestedAgentId
    ) {
        Long dashboardOwnerId = resolveQueueStatsOwnerId(
            requestedAgentId
        );

        return QueueStatsDto.builder()
                            .assignedCount(supportTicketRepository.countAssignedTicketsInQueue(dashboardOwnerId))
                            .inProgressCount(supportTicketRepository.countInProgressTickets(dashboardOwnerId))
                            .resolvedCount(supportTicketRepository.countResolvedTickets(dashboardOwnerId))
                            .escalatedCount(supportTicketRepository.countEscalatedTickets(dashboardOwnerId))
                            .awaitingCustomerCount(supportTicketRepository.countAwaitingCustomerTickets(dashboardOwnerId))
                            .triagingCount(supportTicketRepository.countTriagingTickets(dashboardOwnerId))
                            .build();
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(
        Long requestedUserId
    ) {
        Long dashboardOwnerId = resolveDashboardOwnerId(
            requestedUserId
        );

        return notificationService.getUnreadCount(
            dashboardOwnerId
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getRecentNotifications(
        Long requestedUserId
    ) {
        Long dashboardOwnerId = resolveDashboardOwnerId(
            requestedUserId
        );

        List<Notification> notifications = notificationService.getRecentNotifications(
            dashboardOwnerId
        );

        return notifications.stream()
                            .map(notification -> NotificationDto.builder()
                                                                .id(notification.getId())
                                                                .title(notification.getTitle())
                                                                .body(notification.getBody())
                                                                .type(
                                                                    Objects.isNull(notification.getNotificationType())
                                                                        ? null
                                                                        : notification.getNotificationType().name()
                                                                )
                                                                .ticketId(
                                                                    Objects.isNull(notification.getTicket())
                                                                        ? null
                                                                        : notification.getTicket().getId()
                                                                )
                                                                .link(notification.getLink())
                                                                .read(notification.isRead())
                                                                .createdAt(notification.getCreatedAt())
                                                                .build())
                            .toList();
    }

    @Transactional
    public Long markAllNotificationsAsRead(
        Long requestedUserId
    ) {
        Long dashboardOwnerId = resolveDashboardOwnerId(
            requestedUserId
        );

        notificationService.markAllAsRead(
            dashboardOwnerId
        );

        return dashboardOwnerId;
    }

    @Transactional(readOnly = true)
    public long getEscalationCount() {
        return escalationRepository.countByResolvedFalse();
    }

    private Long resolveDashboardOwnerId(
        Long requestedOwnerId
    ) {
        AppUser actor = Utils.getLoggedInUserDetails();
        Long actorId = actor.getId();

        if (Objects.isNull(requestedOwnerId) || Objects.equals(requestedOwnerId, actorId)) {
            return actorId;
        }

        if (actor.isAdmin() || actor.isSupervisor()) {
            return requireUser(
                requestedOwnerId
            ).getId();
        }

        throw new AccessDeniedException(
            "Not authorized to access another user's dashboard data."
        );
    }

    private Long resolveQueueStatsOwnerId(
        Long requestedOwnerId
    ) {
        Long dashboardOwnerId = resolveDashboardOwnerId(
            requestedOwnerId
        );

        AppUser dashboardOwner = requireUser(
            dashboardOwnerId
        );
        if (!dashboardOwner.canAccessAgentPortal()) {
            throw new AccessDeniedException(
                "Queue stats are only available for staff users."
            );
        }

        return dashboardOwnerId;
    }

    private AppUser requireUser(
        Long userId
    ) {
        return appUserRepository.findById(userId)
                                .orElseThrow(
                                    DataNotFoundException.supplier(
                                        AppUser.class,
                                        userId
                                    )
                                );
    }
}
