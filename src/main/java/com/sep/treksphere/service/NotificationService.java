package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.NotificationCreateCommand;
import com.sep.treksphere.dto.response.NotificationResponse;
import com.sep.treksphere.dto.response.NotificationUnreadCountResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    PaginationResponse<NotificationResponse> getMyNotifications(
            int page,
            int size,
            Boolean isRead,
            CustomUserDetails userDetails
    );

    NotificationUnreadCountResponse getUnreadCount(CustomUserDetails userDetails);

    void markAsRead(UUID notificationId, CustomUserDetails userDetails);

    void markAllAsRead(CustomUserDetails userDetails);

    void deleteNotification(UUID notificationId, CustomUserDetails userDetails);

    NotificationResponse createNotification(NotificationCreateCommand command);

    List<NotificationResponse> createNotifications(List<NotificationCreateCommand> commands);

    List<NotificationResponse> createAdminNotifications(NotificationCreateCommand template);
}
