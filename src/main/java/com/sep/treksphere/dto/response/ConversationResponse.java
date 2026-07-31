package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.chat.ConversationType;
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
public class ConversationResponse {

    private UUID conversationId;
    private String title;
    private String avatarUrl;
    private ConversationType conversationType;
    private LocalDateTime lastMessageAt;
    private String lastMessageContent;
    private Long unreadCount;
}
