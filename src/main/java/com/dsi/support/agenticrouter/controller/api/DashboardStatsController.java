package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.NotificationDto;
import com.dsi.support.agenticrouter.dto.api.QueueStatsDto;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.NotificationService;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/dashboard-stats")
@RequiredArgsConstructor
public class DashboardStatsController {

    private final SupportTicketRepository supportTicketRepository;
    private final EscalationRepository escalationRepository;
    private final NotificationService notificationService;

    @GetMapping("/queue-count")
    public QueueStatsDto getQueueStats(
        @RequestParam(required = false) Long agentId
    ) {
        Long dashboardOwnerId = Objects.requireNonNullElse(
            agentId,
            Utils.getLoggedInUserId()
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

    @GetMapping("/notifications-count")
    public long getUnreadNotificationCount(
        @RequestParam(required = false) Long userId
    ) {
        Long dashboardOwnerId = Objects.requireNonNullElse(
            userId,
            Utils.getLoggedInUserId()
        );

        return notificationService.getUnreadCount(dashboardOwnerId);
    }

    @GetMapping("/notifications-recent")
    public List<NotificationDto> getRecentNotifications(
        @RequestParam(required = false) Long userId
    ) {
        Long dashboardOwnerId = Objects.requireNonNullElse(
            userId,
            Utils.getLoggedInUserId()
        );

        return notificationService.getRecentNotifications(dashboardOwnerId)
                                  .stream()
                                  .map(notification -> NotificationDto.builder()
                                                                      .id(notification.getId())
                                                                      .title(notification.getTitle())
                                                                      .body(notification.getBody())
                                                                      .type(Optional.ofNullable(notification.getNotificationType())
                                                                                    .map(Enum::name)
                                                                                    .orElse(null))
                                                                      .ticketId(Optional.ofNullable(notification.getTicket())
                                                                                        .map(ticket -> ticket.getId())
                                                                                        .orElse(null))
                                                                      .link(notification.getLink())
                                                                      .read(notification.isRead())
                                                                      .createdAt(notification.getCreatedAt())
                                                                      .build())
                                  .toList();
    }

    @PostMapping("/notifications-mark-all-read")
    public void markAllNotificationsAsRead(
        @RequestParam(required = false) Long userId
    ) {
        Long dashboardOwnerId = Objects.requireNonNullElse(
            userId,
            Utils.getLoggedInUserId()
        );

        notificationService.markAllAsRead(dashboardOwnerId);
    }

    @GetMapping("/escalations-count")
    public long getEscalationCount() {
        return escalationRepository.countByResolvedFalse();
    }
}
