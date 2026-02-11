package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.NotificationDto;
import com.dsi.support.agenticrouter.dto.QueueStatsDto;
import com.dsi.support.agenticrouter.entity.BaseEntity;
import com.dsi.support.agenticrouter.repository.EscalationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.service.notification.NotificationService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/dashboard-stats")
@RequiredArgsConstructor
@Slf4j
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

        log.debug(
            "DashboardStatsQueue({}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            dashboardOwnerId
        );

        QueueStatsDto queueStatsDto = QueueStatsDto.builder()
                                                   .assignedCount(supportTicketRepository.countAssignedTicketsInQueue(dashboardOwnerId))
                                                   .inProgressCount(supportTicketRepository.countInProgressTickets(dashboardOwnerId))
                                                   .resolvedCount(supportTicketRepository.countResolvedTickets(dashboardOwnerId))
                                                   .escalatedCount(supportTicketRepository.countEscalatedTickets(dashboardOwnerId))
                                                   .awaitingCustomerCount(supportTicketRepository.countAwaitingCustomerTickets(dashboardOwnerId))
                                                   .triagingCount(supportTicketRepository.countTriagingTickets(dashboardOwnerId))
                                                   .build();

        log.debug(
            "DashboardStatsQueue({}) Actor(id:{}) Outcome(assigned:{},inProgress:{},resolved:{},escalated:{},awaitingCustomer:{},triaging:{})",
            OperationalLogContext.PHASE_COMPLETE,
            dashboardOwnerId,
            queueStatsDto.getAssignedCount(),
            queueStatsDto.getInProgressCount(),
            queueStatsDto.getResolvedCount(),
            queueStatsDto.getEscalatedCount(),
            queueStatsDto.getAwaitingCustomerCount(),
            queueStatsDto.getTriagingCount()
        );

        return queueStatsDto;
    }

    @GetMapping("/notifications-count")
    public long getUnreadNotificationCount(
        @RequestParam(required = false) Long userId
    ) {
        Long dashboardOwnerId = Objects.requireNonNullElse(
            userId,
            Utils.getLoggedInUserId()
        );

        long unreadCount = notificationService.getUnreadCount(dashboardOwnerId);

        log.debug(
            "DashboardStatsNotificationsCount({}) Actor(id:{}) Outcome(unreadCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            dashboardOwnerId,
            unreadCount
        );

        return unreadCount;
    }

    @GetMapping("/notifications-recent")
    public List<NotificationDto> getRecentNotifications(
        @RequestParam(required = false) Long userId
    ) {
        Long dashboardOwnerId = Objects.requireNonNullElse(
            userId,
            Utils.getLoggedInUserId()
        );

        List<NotificationDto> notificationDtos = notificationService.getRecentNotifications(dashboardOwnerId)
                                                                    .stream()
                                                                    .map(notification -> NotificationDto.builder()
                                                                                                        .id(notification.getId())
                                                                                                        .title(notification.getTitle())
                                                                                                        .body(notification.getBody())
                                                                                                        .type(Optional.ofNullable(notification.getNotificationType())
                                                                                                                      .map(Enum::name)
                                                                                                                      .orElse(null))
                                                                                                        .ticketId(Optional.ofNullable(notification.getTicket())
                                                                                                                          .map(BaseEntity::getId)
                                                                                                                          .orElse(null))
                                                                                                        .link(notification.getLink())
                                                                                                        .read(notification.isRead())
                                                                                                        .createdAt(notification.getCreatedAt())
                                                                                                        .build())
                                                                    .toList();

        log.debug(
            "DashboardStatsNotificationsRecent({}) Actor(id:{}) Outcome(resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            dashboardOwnerId,
            notificationDtos.size()
        );

        return notificationDtos;
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

        log.info(
            "DashboardStatsNotificationsMarkAllRead({}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            dashboardOwnerId
        );
    }

    @GetMapping("/escalations-count")
    public long getEscalationCount() {
        long escalationCount = escalationRepository.countByResolvedFalse();

        log.debug(
            "DashboardStatsEscalationCount({}) Outcome(count:{})",
            OperationalLogContext.PHASE_COMPLETE,
            escalationCount
        );

        return escalationCount;
    }
}
