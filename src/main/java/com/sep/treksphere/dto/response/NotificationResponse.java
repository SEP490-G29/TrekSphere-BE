package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
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
}
