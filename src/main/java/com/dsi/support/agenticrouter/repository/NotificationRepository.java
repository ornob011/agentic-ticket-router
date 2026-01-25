package com.dsi.support.agenticrouter.repository;

import com.dsi.support.agenticrouter.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    long countByRecipientIdAndReadFalse(
        Long recipientId
    );
}
