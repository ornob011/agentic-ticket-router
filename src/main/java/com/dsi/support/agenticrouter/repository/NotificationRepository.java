package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndRead(Long recipientId, boolean read);

    List<Notification> findTop20ByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    long deleteByReadTrueAndCreatedAtBefore(Instant before);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Notification notification
        SET notification.read = true
        WHERE notification.recipient.id = :userId
          AND notification.read = false
        """)
    int markAllAsReadByRecipientId(@Param("userId") Long userId);
}
