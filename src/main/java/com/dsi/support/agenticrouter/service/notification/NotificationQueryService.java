package com.dsi.support.agenticrouter.service.notification;

import com.dsi.support.agenticrouter.entity.Notification;
import com.dsi.support.agenticrouter.repository.NotificationRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(
        Long userId
    ) {
        Objects.requireNonNull(userId, "userId");

        List<Notification> notifications = notificationRepository.findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(
            userId
        );

        log.debug(
            "NotificationUnreadList({}) Actor(userId:{}) Outcome(resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            userId,
            notifications.size()
        );

        return notifications;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(
        Long userId
    ) {
        Objects.requireNonNull(userId, "userId");

        long unreadCount = notificationRepository.countByRecipientIdAndRead(userId, Boolean.FALSE);

        log.debug(
            "NotificationUnreadCount({}) Actor(userId:{}) Outcome(unreadCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            userId,
            unreadCount
        );

        return unreadCount;
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(
        Long userId
    ) {
        Objects.requireNonNull(userId, "userId");

        List<Notification> notifications = notificationRepository.findTop20ByRecipient_IdOrderByCreatedAtDesc(
            userId
        );

        log.debug(
            "NotificationRecentList({}) Actor(userId:{}) Outcome(resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            userId,
            notifications.size()
        );

        return notifications;
    }
}
