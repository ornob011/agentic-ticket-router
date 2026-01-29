package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.Notification;
import com.dsi.support.agenticrouter.entity.SupportTicket;
import com.dsi.support.agenticrouter.enums.NotificationType;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.NotificationRepository;
import com.dsi.support.agenticrouter.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO: Fix the class

/**
 * Service for managing in-app notifications.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final SupportTicketRepository supportTicketRepository;

    /**
     * Create a new notification for a user.
     *
     * @param recipientId User ID to receive notification
     * @param type        Notification type
     * @param title       Notification title
     * @param body        Notification body
     * @param ticketId    Related ticket ID (nullable)
     * @return Created notification
     */
    public Notification createNotification(
        Long recipientId,
        NotificationType type,
        String title,
        String body,
        Long ticketId
    ) {
        log.debug("Creating notification: recipientId={}, type={}, title={}",
            recipientId, type, title);

        AppUser recipient = appUserRepository.findById(recipientId)
                                             .orElseThrow(() -> new IllegalArgumentException("User not found: " + recipientId));

        SupportTicket ticket = ticketId != null ? supportTicketRepository.findById(ticketId).orElse(null) : null;

        Notification notification = Notification.builder()
                                                .recipient(recipient)
                                                .notificationType(type)
                                                .title(title)
                                                .body(body)
                                                .ticket(ticket)
                                                .read(false)
                                                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Get unread notifications for a user.
     *
     * @param userId User ID
     * @return List of unread notifications
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Mark a notification as read.
     *
     * @param notificationId Notification ID
     * @param userId         User ID (for authorization check)
     */
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId)
                              .ifPresent(notification -> {
                                  if (notification.getRecipient().getId().equals(userId)) {
                                      notification.setRead(true);
                                      notificationRepository.save(notification);
                                      log.debug("Marked notification {} as read for user {}", notificationId, userId);
                                  } else {
                                      log.warn("User {} attempted to mark notification {} belonging to another user",
                                          userId, notificationId);
                                  }
                              });
    }

    /**
     * Get notification count for a user.
     *
     * @param userId User ID
     * @return Count of unread notifications
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }
}
