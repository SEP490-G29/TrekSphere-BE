package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateCommand {
    private UUID recipientId;
    private String title;
    private String content;
    private NotificationEventType eventType;
    private ReferenceType referenceType;
    private UUID referenceId;
    private String actionUrl;
}
