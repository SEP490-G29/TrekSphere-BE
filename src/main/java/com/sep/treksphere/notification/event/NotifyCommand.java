package com.sep.treksphere.notification.event;

import com.sep.treksphere.notification.enums.NotificationEventType;
import com.sep.treksphere.notification.enums.ReferenceType;

import java.util.List;
import java.util.UUID;

public record NotifyCommand(
        List<UUID> recipientIds,
        NotificationEventType eventType,
        String title,
        String content,
        ReferenceType referenceType,
        UUID referenceId,
        String actionUrl
) {
}
