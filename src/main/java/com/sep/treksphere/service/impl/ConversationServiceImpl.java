package com.sep.treksphere.service.impl;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.ConversationCreateRequest;
import com.sep.treksphere.dto.request.MessageCreateRequest;
import com.sep.treksphere.dto.response.ConversationResponse;
import com.sep.treksphere.dto.response.MessageResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.UserResponse;
import com.sep.treksphere.entity.Conversation;
import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.Message;
import com.sep.treksphere.entity.User;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import com.sep.treksphere.enums.chat.ConversationType;
import com.sep.treksphere.enums.user.UserStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.ConversationRepository;
import com.sep.treksphere.repository.MatchingGroupRepository;
import com.sep.treksphere.repository.MessageRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final SimpMessagingTemplate messagingTemplate;

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

    @Override
    @Transactional
    public ConversationResponse createConversation(
            ConversationCreateRequest request,
            CustomUserDetails userDetails
    ) {
        User currentUser = userDetails.getUser();
        UUID currentUserId = currentUser.getUserId();
        Set<UUID> participantIds = new LinkedHashSet<>(request.getParticipantIds());

        if (participantIds.contains(currentUserId)) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        validateParticipantCount(request.getConversationType(), participantIds.size());
        String title = validateAndNormalizeTitle(request);

        List<User> participants = userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(
                participantIds,
                UserStatus.ACTIVE
        );
        if (participants.size() != participantIds.size()) {
            throw new AppException(ErrorCode.RECIPIENT_NOT_FOUND);
        }

        if (request.getConversationType() == ConversationType.DIRECT) {
            UUID recipientId = participantIds.iterator().next();
            Optional<Conversation> existing = conversationRepository.findDirectConversation(currentUserId, recipientId);
            if (existing.isPresent()) {
                return toCreatedConversationResponse(existing.get(), currentUserId, false);
            }
        } else if (request.getConversationType() == ConversationType.GROUP && request.getMatchingGroupId() != null) {
            MatchingGroup matchingGroup = matchingGroupRepository.findById(request.getMatchingGroupId())
                    .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));
            if (matchingGroup.getConversation() != null) {
                return toCreatedConversationResponse(matchingGroup.getConversation(), currentUserId, false);
            }
        }

        Conversation conversation = new Conversation();
        conversation.setConversationType(request.getConversationType());
        conversation.setTitle(title);
        conversation.getParticipants().add(currentUser);
        conversation.getParticipants().addAll(participants);

        Conversation savedConversation = conversationRepository.save(conversation);

        if (request.getConversationType() == ConversationType.GROUP && request.getMatchingGroupId() != null) {
            MatchingGroup matchingGroup = matchingGroupRepository.findById(request.getMatchingGroupId())
                    .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));
            matchingGroup.setConversation(savedConversation);
            matchingGroupRepository.save(matchingGroup);
        }

        return toCreatedConversationResponse(savedConversation, currentUserId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse checkConversation(ConversationCreateRequest request, CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        Set<UUID> participantIds = new LinkedHashSet<>(request.getParticipantIds());

        if (request.getConversationType() == ConversationType.DIRECT) {
            if (!participantIds.isEmpty()) {
                UUID recipientId = participantIds.iterator().next();
                Optional<Conversation> existing = conversationRepository.findDirectConversation(currentUserId, recipientId);
                return existing.map(c -> toCreatedConversationResponse(c, currentUserId, false)).orElse(null);
            }
        } else if (request.getConversationType() == ConversationType.GROUP && request.getMatchingGroupId() != null) {
            MatchingGroup matchingGroup = matchingGroupRepository.findById(request.getMatchingGroupId()).orElse(null);
            if (matchingGroup != null && matchingGroup.getConversation() != null) {
                return toCreatedConversationResponse(matchingGroup.getConversation(), currentUserId, false);
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MessageResponse> getMessages(
            UUID conversationId,
            int page,
            int size,
            CustomUserDetails userDetails
    ) {
        UUID currentUserId = userDetails.getUser().getUserId();
        conversationRepository.findActiveConversationByIdAndParticipantId(
                conversationId,
                currentUserId
        ).orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        Page<Message> messagePage =
                messageRepository.findByConversationConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        conversationId,
                        PageRequest.of(page - 1, size)
                );

        return PaginationResponse.<MessageResponse>builder()
                .content(messagePage.getContent().stream()
                        .map(this::toMessageResponse)
                        .toList())
                .pageNumber(messagePage.getNumber() + 1)
                .pageSize(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .last(messagePage.isLast())
                .build();
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(
            MessageCreateRequest request,
            CustomUserDetails userDetails
    ) {
        User currentUser = userDetails.getUser();
        Conversation conversation = conversationRepository
                .findActiveConversationByIdAndParticipantId(
                        request.getConversationId(),
                        currentUser.getUserId()
                )
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(currentUser);
        message.setContent(request.getContent().trim());
        message.setIsRead(false);

        Message savedMessage = messageRepository.saveAndFlush(message);
        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        conversationRepository.save(conversation);

        MessageResponse response = toMessageResponse(savedMessage);
        broadcastMessageAfterCommit(response);
        return response;
    }

    @Override
    @Transactional
    public void markMessagesAsRead(
            UUID conversationId,
            CustomUserDetails userDetails
    ) {
        UUID currentUserId = userDetails.getUser().getUserId();
        conversationRepository.findActiveConversationByIdAndParticipantId(
                conversationId,
                currentUserId
        ).orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        messageRepository.markMessagesAsRead(conversationId, currentUserId);
    }

    @Override
    @Transactional
    public void deleteConversation(UUID conversationId, CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (conversation.getIsDeleted()) {
            throw new AppException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        if (conversation.getConversationType() == ConversationType.DIRECT) {
            boolean isParticipant = conversation.getParticipants().stream()
                    .anyMatch(p -> p.getUserId().equals(currentUserId));
            if (!isParticipant) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        } else if (conversation.getConversationType() == ConversationType.GROUP) {
            if (conversation.getCreatedBy() == null || !conversation.getCreatedBy().equals(currentUserId.toString())) {
                throw new AppException(ErrorCode.ACCESS_DENIED, "Chỉ trưởng nhóm mới có quyền xóa nhóm chat");
            }
        }

        // Unlink from MatchingGroup if necessary
        matchingGroupRepository.findByConversationConversationId(conversationId).ifPresent(mg -> {
            mg.setConversation(null);
            matchingGroupRepository.save(mg);
        });

        conversation.setIsDeleted(true);
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public void removeMember(UUID conversationId, UUID memberId, CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (conversation.getIsDeleted()) {
            throw new AppException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        if (conversation.getConversationType() != ConversationType.GROUP) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể xóa thành viên khỏi chat 1-1");
        }

        if (conversation.getCreatedBy() == null || !conversation.getCreatedBy().equals(currentUserId.toString())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Chỉ trưởng nhóm mới có quyền xóa thành viên");
        }

        if (currentUserId.equals(memberId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể tự xóa bản thân bằng chức năng này");
        }

        User memberToRemove = conversation.getParticipants().stream()
                .filter(p -> p.getUserId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Thành viên không có trong nhóm chat"));

        conversation.getParticipants().remove(memberToRemove);
        conversationRepository.save(conversation);
    }

    private void broadcastMessageAfterCommit(MessageResponse response) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        messagingTemplate.convertAndSend(
                                "/topic/chat/conversations/"
                                        + response.getConversationId()
                                        + "/messages",
                                response
                        );
                    }
                }
        );
    }

    private void validateParticipantCount(ConversationType conversationType, int participantCount) {
        if (conversationType == ConversationType.DIRECT && participantCount != 1) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    MessageConstant.DIRECT_PARTICIPANT_COUNT_INVALID
            );
        }

        if (conversationType == ConversationType.GROUP && participantCount < 2) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    MessageConstant.GROUP_PARTICIPANT_COUNT_INVALID
            );
        }
    }

    private String validateAndNormalizeTitle(ConversationCreateRequest request) {
        if (request.getConversationType() == ConversationType.DIRECT) {
            return null;
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    MessageConstant.GROUP_TITLE_REQUIRED
            );
        }
        return request.getTitle().trim();
    }

    private ConversationResponse toCreatedConversationResponse(
            Conversation conversation,
            UUID currentUserId,
            boolean isNew
    ) {
        User otherParticipant = conversation.getConversationType() == ConversationType.DIRECT
                ? conversation.getParticipants().stream()
                        .filter(participant -> !participant.getUserId().equals(currentUserId))
                        .findFirst()
                        .orElse(null)
                : null;

        boolean isGroupLeader = conversation.getConversationType() == ConversationType.GROUP 
                && conversation.getCreatedBy() != null 
                && conversation.getCreatedBy().equals(currentUserId.toString());

        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .title(otherParticipant != null
                        ? otherParticipant.getFullName()
                        : conversation.getTitle())
                .avatarUrl(otherParticipant != null
                        ? otherParticipant.getAvatarUrl()
                        : null)
                .conversationType(conversation.getConversationType())
                .lastMessageAt(null)
                .lastMessageContent(null)
                .unreadCount(0L)
                .isNew(isNew)
                .isGroupLeader(isGroupLeader)
                .build();
    }

    @Override
    public List<UserResponse> getConversationMembers(UUID conversationId, CustomUserDetails userDetails) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (conversation.getIsDeleted()) {
            throw new AppException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        // Verify user is in conversation
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userDetails.getUser().getUserId()));
        
        if (!isParticipant) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Không có quyền xem thành viên nhóm này");
        }

        return conversation.getParticipants().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getUserId().toString())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .collect(Collectors.toList());
    }

    private MessageResponse toMessageResponse(Message message) {
        User sender = message.getSender();

        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversation().getConversationId())
                .senderId(sender.getUserId())
                .senderName(sender.getFullName())
                .senderAvatarUrl(sender.getAvatarUrl())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
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
        
        boolean isGroupLeader = conversation.getConversationType() == ConversationType.GROUP 
                && conversation.getCreatedBy() != null 
                && conversation.getCreatedBy().equals(currentUserId.toString());

        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .title(title)
                .avatarUrl(avatarUrl)
                .conversationType(conversation.getConversationType())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastMessageContent(lastMessageContent)
                .unreadCount(unreadCount)
                .isGroupLeader(isGroupLeader)
                .build();
    }
}
