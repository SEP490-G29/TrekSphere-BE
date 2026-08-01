package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
