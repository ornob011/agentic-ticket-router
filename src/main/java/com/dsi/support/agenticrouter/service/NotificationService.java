package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Notification;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.NotificationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
            "NotificationCreate({}) Actor(recipientId:{}) SupportTicket(id:{}) Notification(type:{},titleLength:{},bodyLength:{})",
            OperationalLogContext.PHASE_START,
            recipientId,
            ticketId,
            type,
            StringUtils.length(title),
            StringUtils.length(body)
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
            "NotificationCreate({}) Notification(id:{},type:{},read:{}) Actor(recipientId:{}) SupportTicket(id:{})",
            OperationalLogContext.PHASE_COMPLETE,
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
            "NotificationUnreadList({}) Actor(userId:{}) Outcome(resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
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
            "NotificationMarkRead({}) Notification(id:{}) Actor(userId:{})",
            OperationalLogContext.PHASE_START,
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
                                              "NotificationMarkRead({}) Notification(id:{},read:{}) Actor(userId:{})",
                                              OperationalLogContext.PHASE_COMPLETE,
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
            "NotificationUnreadCount({}) Actor(userId:{}) Outcome(unreadCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
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
            "NotificationDeleteRead({}) Outcome(before:{},deletedCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
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
            "NotificationRecentList({}) Actor(userId:{}) Outcome(resultCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
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
            "NotificationMarkAllRead({}) Actor(userId:{}) Outcome(updatedCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            userId,
            unreadNotifications.size()
        );
    }
}
