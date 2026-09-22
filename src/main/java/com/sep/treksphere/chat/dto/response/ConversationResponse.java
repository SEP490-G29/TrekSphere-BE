package com.sep.treksphere.chat.dto.response;

import com.sep.treksphere.chat.enums.ConversationType;
import lombok.*;

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
    private Boolean isNew;
    private Boolean isGroupLeader;
}
