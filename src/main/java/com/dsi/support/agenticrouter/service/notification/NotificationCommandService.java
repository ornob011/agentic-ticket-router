package com.dsi.support.agenticrouter.service.notification;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Notification;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.NotificationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final SupportTicketRepository supportTicketRepository;

    @Transactional
    public void createNotification(
        Long recipientId,
        NotificationType type,
        String title,
        String body,
        Long ticketId
    ) {
        log.debug(
            "NotificationCreate({}) Actor(recipientId:{}) SupportTicket(id:{}) Notification(type:{},titleLength:{},bodyLength:{})",
            OperationalLogContext.PHASE_START,
            recipientId,
            ticketId,
            type,
            StringUtils.length(title),
            StringUtils.length(body)
        );

        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(ticketId, "ticketId");

        String normalizedTitle = StringNormalizationUtils.trimToNull(title);
        String normalizedBody = StringNormalizationUtils.trimToNull(body);
        NotificationType normalizedType = type == null ? NotificationType.STATUS_CHANGE : type;

        if (normalizedTitle == null) {
            normalizedTitle = "Ticket Update";
        }

        if (normalizedBody == null) {
            normalizedBody = "There has been an update to your ticket.";
        }

        AppUser recipient = appUserRepository.findById(recipientId)
                                             .orElseThrow(
                                                 DataNotFoundException.supplier(
                                                     AppUser.class,
                                                     recipientId
                                                 )
                                             );

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                                                             .orElseThrow(
                                                                 DataNotFoundException.supplier(
                                                                     SupportTicket.class,
                                                                     ticketId
                                                                 )
                                                             );

        Notification notification = Notification.builder()
                                                .recipient(recipient)
                                                .notificationType(normalizedType)
                                                .title(normalizedTitle)
                                                .body(normalizedBody)
                                                .ticket(supportTicket)
                                                .read(false)
                                                .build();

        notificationRepository.save(notification);

        log.info(
            "NotificationCreate({}) Notification(id:{},type:{},read:{}) Actor(recipientId:{}) SupportTicket(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
            notification.getId(),
            notification.getNotificationType(),
            notification.isRead(),
            recipientId,
            ticketId
        );
    }

    @Transactional
    public void markAsRead(
        Long notificationId,
        Long userId
    ) {
        Objects.requireNonNull(notificationId, "notificationId");
        Objects.requireNonNull(userId, "userId");

        log.debug(
            "NotificationMarkRead({}) Notification(id:{}) Actor(userId:{})",
            OperationalLogContext.PHASE_START,
            notificationId,
            userId
        );

        Optional<Notification> optionalNotification = notificationRepository.findById(
            notificationId
        );

        if (optionalNotification.isEmpty()) {
            log.debug(
                "NotificationMarkRead({}) Notification(id:{}) Actor(userId:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                notificationId,
                userId,
                "notification_not_found"
            );
            return;
        }

        Notification notification = optionalNotification.get();
        if (!Objects.equals(notification.getRecipient().getId(), userId)) {
            log.warn(
                "NotificationMarkRead({}) Notification(id:{}) Actor(userId:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                notificationId,
                userId,
                "recipient_mismatch"
            );
            return;
        }

        if (notification.isRead()) {
            log.debug(
                "NotificationMarkRead({}) Notification(id:{},read:{}) Actor(userId:{}) Outcome(reason:{})",
                OperationalLogContext.PHASE_SKIP,
                notification.getId(),
                notification.isRead(),
                userId,
                "already_read"
            );
            return;
        }

        notification.setRead(true);
        notificationRepository.save(notification);

        log.info(
            "NotificationMarkRead({}) Notification(id:{},read:{}) Actor(userId:{})",
            OperationalLogContext.PHASE_COMPLETE,
            notification.getId(),
            notification.isRead(),
            userId
        );
    }

    @Transactional
    public void deleteReadNotificationsOlderThan(
        Instant before
    ) {
        Objects.requireNonNull(before, "before");

        long deletedCount = notificationRepository.deleteByReadTrueAndCreatedAtBefore(
            before
        );

        log.info(
            "NotificationDeleteRead({}) Outcome(before:{},deletedCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            before,
            deletedCount
        );
    }

    @Transactional
    public void markAllAsRead(
        Long userId
    ) {
        Objects.requireNonNull(userId, "userId");

        int updatedCount = notificationRepository.markAllAsReadByRecipientId(
            userId
        );

        log.info(
            "NotificationMarkAllRead({}) Actor(userId:{}) Outcome(updatedCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            userId,
            updatedCount
        );
    }
}
