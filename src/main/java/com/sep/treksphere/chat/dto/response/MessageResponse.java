package com.sep.treksphere.chat.dto.response;

import com.sep.treksphere.chat.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String content;
    private MessageType messageType;
    private AttachmentResponse attachment;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
