package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipient_UserIdAndIsDeletedFalse(UUID recipientId, Pageable pageable);

    Page<Notification> findByRecipient_UserIdAndIsReadAndIsDeletedFalse(
            UUID recipientId, Boolean isRead, Pageable pageable);

    long countByRecipient_UserIdAndIsReadFalseAndIsDeletedFalse(UUID recipientId);

    java.util.Optional<Notification> findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(
            UUID notificationId, UUID recipientId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP " +
            "where n.recipient.userId = :recipientId and n.isRead = false and n.isDeleted = false")
    int markAllRead(@Param("recipientId") UUID recipientId);
}
