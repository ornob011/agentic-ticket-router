package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.NotificationDto;
import com.dsi.support.agenticrouter.dto.QueueStatsDto;
import com.dsi.support.agenticrouter.service.dashboard.DashboardStatsService;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard-stats")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/queue-count")
    @PreAuthorize("hasAnyRole('AGENT','SUPERVISOR','ADMIN')")
    public QueueStatsDto getQueueStats(
        @RequestParam(required = false) Long agentId
    ) {
        QueueStatsDto queueStatsDto = dashboardStatsService.getQueueStats(
            agentId
        );

        log.debug(
            "DashboardStatsQueue({}) Outcome(assigned:{},inProgress:{},resolved:{},escalated:{},awaitingCustomer:{},triaging:{})",
            OperationalLogContext.PHASE_COMPLETE,
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
        long unreadCount = dashboardStatsService.getUnreadNotificationCount(
            userId
        );

        log.debug(
            "DashboardStatsNotificationsCount({}) Outcome(unreadCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            unreadCount
        );

        return unreadCount;
    }

    @GetMapping("/notifications-recent")
    public List<NotificationDto> getRecentNotifications(
        @RequestParam(required = false) Long userId
    ) {
        List<NotificationDto> notificationDtos = dashboardStatsService.getRecentNotifications(
            userId
        );

        log.debug(
            "DashboardStatsNotificationsRecent({}) Outcome(resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            notificationDtos.size()
        );

        return notificationDtos;
    }

    @PostMapping("/notifications-mark-all-read")
    public void markAllNotificationsAsRead(
        @RequestParam(required = false) Long userId
    ) {
        Long actorId = dashboardStatsService.markAllNotificationsAsRead(
            userId
        );

        log.info(
            "DashboardStatsNotificationsMarkAllRead({}) Actor(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            actorId
        );
    }

    @GetMapping("/escalations-count")
    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    public long getEscalationCount() {
        long escalationCount = dashboardStatsService.getEscalationCount();

        log.debug(
            "DashboardStatsEscalationCount({}) Outcome(count:{})",
            OperationalLogContext.PHASE_COMPLETE,
            escalationCount
        );

        return escalationCount;
    }
}
