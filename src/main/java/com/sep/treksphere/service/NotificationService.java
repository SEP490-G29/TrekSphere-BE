package com.sep.treksphere.service;

import com.sep.treksphere.dto.response.NotificationResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
    NotificationResponse create(User recipient, String title, String content,
                                NotificationEventType eventType, ReferenceType referenceType,
                                UUID referenceId, String actionUrl);

    PaginationResponse<NotificationResponse> getMyNotifications(UUID recipientId, Boolean isRead, Pageable pageable);

    long getUnreadCount(UUID recipientId);

    NotificationResponse markAsRead(UUID recipientId, UUID notificationId);

    int markAllAsRead(UUID recipientId);
}
