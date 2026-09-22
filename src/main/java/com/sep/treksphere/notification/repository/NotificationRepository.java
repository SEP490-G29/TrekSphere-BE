package com.sep.treksphere.notification;

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

    Page<Notification> findByRecipient_UserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<Notification> findByRecipient_UserIdAndIsReadAndIsDeletedFalse(
            UUID userId, Boolean isRead, Pageable pageable);

    long countByRecipient_UserIdAndIsReadFalseAndIsDeletedFalse(UUID userId);

    Optional<Notification> findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(
            UUID notificationId, UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.recipient.userId = :userId AND n.isRead = false AND n.isDeleted = false")
    void markAllAsRead(@Param("userId") UUID userId);
}
