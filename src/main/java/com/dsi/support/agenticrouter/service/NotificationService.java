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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
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
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(
        Long userId
    ) {
        return notificationRepository.findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(
            userId
        );
    }

    public void markAsRead(
        Long notificationId,
        Long userId
    ) {
        notificationRepository.findById(notificationId)
                              .ifPresent(
                                  notification -> {
                                      if (notification.getRecipient().getId().equals(userId)) {
                                          notification.setRead(true);
                                          notificationRepository.save(notification);
                                      }
                                  }
                              );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(
        Long userId
    ) {
        return notificationRepository.countByRecipientIdAndRead(userId, Boolean.FALSE);
    }

    @Transactional
    public void deleteReadNotificationsOlderThan(
        Instant before
    ) {
        List<Notification> oldNotifications = notificationRepository.findOldReadNotifications(
            before
        );

        notificationRepository.deleteAll(oldNotifications);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(
        Long userId
    ) {
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId);
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
    }
}
