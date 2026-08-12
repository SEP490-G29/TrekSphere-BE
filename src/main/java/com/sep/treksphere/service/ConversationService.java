package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.ConversationCreateRequest;
import com.sep.treksphere.dto.request.MessageCreateRequest;
import com.sep.treksphere.dto.response.ConversationResponse;
import com.sep.treksphere.dto.response.MessageResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface ConversationService {

    PaginationResponse<ConversationResponse> getConversations(
            int page,
            int size,
            CustomUserDetails userDetails
    );

    ConversationResponse createConversation(
            ConversationCreateRequest request,
            CustomUserDetails userDetails
    );

    ConversationResponse checkConversation(
            ConversationCreateRequest request,
            CustomUserDetails userDetails
    );

    PaginationResponse<MessageResponse> getMessages(
            UUID conversationId,
            int page,
            int size,
            CustomUserDetails userDetails
    );

    MessageResponse sendMessage(
            MessageCreateRequest request,
            CustomUserDetails userDetails
    );

    void markMessagesAsRead(
            UUID conversationId,
            CustomUserDetails userDetails
    );
}
