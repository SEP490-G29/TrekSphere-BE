package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipient.userId = :recipientId
          AND n.isDeleted = false
          AND (:isRead IS NULL OR n.isRead = :isRead)
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findInbox(
            @Param("recipientId") UUID recipientId,
            @Param("isRead") Boolean isRead,
            Pageable pageable
    );

    Optional<Notification> findByNotificationIdAndRecipientUserIdAndIsDeletedFalse(
            UUID notificationId,
            UUID recipientId
    );

    long countByRecipientUserIdAndIsReadFalseAndIsDeletedFalse(UUID recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.recipient.userId = :recipientId
          AND n.isRead = false
          AND n.isDeleted = false
    """)
    int markAllAsRead(@Param("recipientId") UUID recipientId);
}
