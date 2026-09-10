package com.sep.treksphere.notification;

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
