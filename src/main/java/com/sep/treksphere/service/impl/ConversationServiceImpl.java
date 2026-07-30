package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.response.ConversationResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.Conversation;
import com.sep.treksphere.entity.Message;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.chat.ConversationType;
import com.sep.treksphere.repository.ConversationRepository;
import com.sep.treksphere.repository.MessageRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ConversationResponse> getConversations(
            int page,
            int size,
            CustomUserDetails userDetails
    ) {
        UUID currentUserId = userDetails.getUser().getUserId();
        Page<Conversation> conversationPage = conversationRepository.findActiveConversationsByUserId(
                currentUserId,
                PageRequest.of(page - 1, size)
        );

        return PaginationResponse.<ConversationResponse>builder()
                .content(conversationPage.getContent().stream()
                        .map(conversation -> toConversationResponse(conversation, currentUserId))
                        .toList())
                .pageNumber(conversationPage.getNumber() + 1)
                .pageSize(conversationPage.getSize())
                .totalElements(conversationPage.getTotalElements())
                .totalPages(conversationPage.getTotalPages())
                .last(conversationPage.isLast())
                .build();
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation,
            UUID currentUserId
    ) {
        User otherParticipant = conversation.getConversationType() == ConversationType.DIRECT
                ? conversation.getParticipants().stream()
                        .filter(participant -> !participant.getUserId().equals(currentUserId))
                        .findFirst()
                        .orElse(null)
                : null;

        String title = otherParticipant != null
                ? otherParticipant.getFullName()
                : conversation.getTitle();
        String avatarUrl = otherParticipant != null
                ? otherParticipant.getAvatarUrl()
                : null;
        String lastMessageContent = messageRepository
                .findFirstByConversationConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        conversation.getConversationId()
                )
                .map(Message::getContent)
                .orElse(null);
        long unreadCount = messageRepository
                .countByConversationConversationIdAndSenderUserIdNotAndIsReadFalseAndIsDeletedFalse(
                        conversation.getConversationId(),
                        currentUserId
                );

        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .title(title)
                .avatarUrl(avatarUrl)
                .conversationType(conversation.getConversationType())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastMessageContent(lastMessageContent)
                .unreadCount(unreadCount)
                .build();
    }
}
