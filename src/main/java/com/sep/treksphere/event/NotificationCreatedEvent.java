package com.sep.treksphere.event;

import com.sep.treksphere.dto.response.NotificationResponse;

public record NotificationCreatedEvent(String recipientEmail, NotificationResponse notification) {
}
