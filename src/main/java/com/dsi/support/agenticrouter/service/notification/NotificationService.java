package com.dsi.support.agenticrouter.service.notification;

import com.dsi.support.agenticrouter.entity.Notification;
import com.dsi.support.agenticrouter.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;

    public void createNotification(
        Long recipientId,
        NotificationType type,
        String title,
        String body,
        Long ticketId
    ) {
        notificationCommandService.createNotification(
            recipientId,
            type,
            title,
            body,
            ticketId
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(
        Long userId
    ) {
        return notificationQueryService.getUnreadNotifications(
            userId
        );
    }

    public void markAsRead(
        Long notificationId,
        Long userId
    ) {
        notificationCommandService.markAsRead(
            notificationId,
            userId
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(
        Long userId
    ) {
        return notificationQueryService.getUnreadCount(
            userId
        );
    }

    @Transactional
    public void deleteReadNotificationsOlderThan(
        Instant before
    ) {
        notificationCommandService.deleteReadNotificationsOlderThan(
            before
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(
        Long userId
    ) {
        return notificationQueryService.getRecentNotifications(
            userId
        );
    }

    @Transactional
    public void markAllAsRead(
        Long userId
    ) {
        notificationCommandService.markAllAsRead(
            userId
        );
    }
}
