package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Notification;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.NotificationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final SupportTicketRepository supportTicketRepository;

    public void createNotification(
        Long recipientId,
        NotificationType type,
        String title,
        String body,
        Long ticketId
    ) {
        log.debug(
            "NotificationCreate(start) Actor(recipientId:{}) SupportTicket(id:{}) Notification(type:{},titleLength:{},bodyLength:{})",
            recipientId,
            ticketId,
            type,
            title != null ? title.length() : 0,
            body != null ? body.length() : 0
        );

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
                                                .notificationType(type)
                                                .title(title)
                                                .body(body)
                                                .ticket(supportTicket)
                                                .read(false)
                                                .build();

        notificationRepository.save(notification);

        log.info(
            "NotificationCreate(complete) Notification(id:{},type:{},read:{}) Actor(recipientId:{}) SupportTicket(id:{})",
            notification.getId(),
            notification.getNotificationType(),
            notification.isRead(),
            recipientId,
            ticketId
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(
        Long userId
    ) {
        List<Notification> notifications = notificationRepository.findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(
            userId
        );

        log.debug(
            "NotificationUnreadList(complete) Actor(userId:{}) Outcome(resultCount:{})",
            userId,
            notifications.size()
        );

        return notifications;
    }

    public void markAsRead(
        Long notificationId,
        Long userId
    ) {
        log.debug(
            "NotificationMarkRead(start) Notification(id:{}) Actor(userId:{})",
            notificationId,
            userId
        );

        notificationRepository.findById(notificationId)
                              .ifPresent(
                                  notification -> {
                                      if (notification.getRecipient().getId().equals(userId)) {
                                          notification.setRead(true);
                                          notificationRepository.save(notification);
                                          log.info(
                                              "NotificationMarkRead(complete) Notification(id:{},read:{}) Actor(userId:{})",
                                              notification.getId(),
                                              notification.isRead(),
                                              userId
                                          );
                                      }
                                  }
                              );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(
        Long userId
    ) {
        long unreadCount = notificationRepository.countByRecipientIdAndRead(userId, Boolean.FALSE);

        log.debug(
            "NotificationUnreadCount(complete) Actor(userId:{}) Outcome(unreadCount:{})",
            userId,
            unreadCount
        );

        return unreadCount;
    }

    @Transactional
    public void deleteReadNotificationsOlderThan(
        Instant before
    ) {
        List<Notification> oldNotifications = notificationRepository.findOldReadNotifications(
            before
        );

        notificationRepository.deleteAll(oldNotifications);

        log.info(
            "NotificationDeleteRead(complete) Outcome(before:{},deletedCount:{})",
            before,
            oldNotifications.size()
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(
        Long userId
    ) {
        List<Notification> notifications = notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId);

        log.debug(
            "NotificationRecentList(complete) Actor(userId:{}) Outcome(resultCount:{})",
            userId,
            notifications.size()
        );

        return notifications;
    }

    @Transactional
    public void markAllAsRead(
        Long userId
    ) {
        List<Notification> unreadNotifications = notificationRepository.findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(
            userId
        );

        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(unreadNotifications);

        log.info(
            "NotificationMarkAllRead(complete) Actor(userId:{}) Outcome(updatedCount:{})",
            userId,
            unreadNotifications.size()
        );
    }
}
