package com.sep.treksphere.notification;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {

    private UUID notificationId;
    private String title;
    private String content;
    private NotificationEventType eventType;
    private ReferenceType referenceType;
    private UUID referenceId;
    private String actionUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .eventType(notification.getEventType())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .actionUrl(notification.getActionUrl())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
