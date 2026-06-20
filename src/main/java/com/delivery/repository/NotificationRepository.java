package com.delivery.repository;

import com.delivery.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Integer recipientId);
    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String username);
    List<Notification> findByRecipientUsernameAndIsReadOrderByCreatedAtDesc(String username, boolean isRead);
    long countByRecipientUsernameAndIsRead(String username, boolean isRead);
}
