package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(Long recipientId);

    @Query("""
        SELECT notification
        FROM Notification notification
        WHERE notification.createdAt < :before AND notification.read = false
        """)
    List<Notification> findOldReadNotifications(@Param("before") Instant before);

    void markAllAsReadByRecipientId(Long recipientId);

    long countByRecipientIdAndRead(Long recipientId, boolean read);

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);
}
