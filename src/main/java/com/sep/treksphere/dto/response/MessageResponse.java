package com.sep.treksphere.dto.response;

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
public class MessageResponse {

    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
