package com.sep.treksphere.service.impl;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.ConversationCreateRequest;
import com.sep.treksphere.dto.response.ConversationResponse;
import com.sep.treksphere.entity.Conversation;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.chat.ConversationType;
import com.sep.treksphere.enums.user.UserStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.ConversationRepository;
import com.sep.treksphere.repository.MessageRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.sep.treksphere.repository.MatchingGroupRepository matchingGroupRepository;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private User currentUser;
    private User recipient;
    private User groupParticipant;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        currentUser = createUser("Người tạo", null);
        recipient = createUser("Người nhận", "recipient-avatar.jpg");
        groupParticipant = createUser("Thành viên nhóm", null);
        userDetails = new CustomUserDetails(currentUser);
    }

    @Test
    void createDirectConversationSuccess() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT,
                "Tên bị bỏ qua",
                List.of(recipient.getUserId()),
                null
        );
        when(userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(
                any(),
                any(UserStatus.class)
        )).thenReturn(List.of(recipient));
        when(conversationRepository.findDirectConversation(
                currentUser.getUserId(),
                recipient.getUserId()
        )).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation conversation = invocation.getArgument(0);
                    conversation.setConversationId(UUID.randomUUID());
                    return conversation;
                });

        ConversationResponse response = conversationService.createConversation(request, userDetails);

        assertEquals(ConversationType.DIRECT, response.getConversationType());
        assertEquals(recipient.getFullName(), response.getTitle());
        assertEquals(recipient.getAvatarUrl(), response.getAvatarUrl());
        assertEquals(0L, response.getUnreadCount());
        assertNull(response.getLastMessageAt());
        assertNull(response.getLastMessageContent());

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        Conversation savedConversation = captor.getValue();
        assertNull(savedConversation.getTitle());
        assertEquals(2, savedConversation.getParticipants().size());
        assertTrue(savedConversation.getParticipants().contains(currentUser));
        assertTrue(savedConversation.getParticipants().contains(recipient));
    }

    @Test
    void createGroupConversationSuccessAndTrimsTitle() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.GROUP,
                "  Nhóm Fansipan  ",
                List.of(
                        recipient.getUserId(),
                        groupParticipant.getUserId(),
                        recipient.getUserId()
                ),
                null
        );
        when(userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(
                any(),
                any(UserStatus.class)
        )).thenReturn(List.of(recipient, groupParticipant));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation conversation = invocation.getArgument(0);
                    conversation.setConversationId(UUID.randomUUID());
                    return conversation;
                });

        ConversationResponse response = conversationService.createConversation(request, userDetails);

        assertEquals(ConversationType.GROUP, response.getConversationType());
        assertEquals("Nhóm Fansipan", response.getTitle());
        assertNull(response.getAvatarUrl());

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getParticipants().size());
    }

    @Test
    void createConversationWithCurrentUserThrowsCannotChatWithSelf() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT,
                null,
                List.of(currentUser.getUserId()),
                null
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> conversationService.createConversation(request, userDetails)
        );

        assertEquals(ErrorCode.CANNOT_CHAT_WITH_SELF, exception.getErrorCode());
        verify(userRepository, never()).findAllByUserIdInAndStatusAndIsDeletedFalse(any(), any());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createExistingDirectConversationReturnsExistingConversation() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT,
                null,
                List.of(recipient.getUserId()),
                null
        );
        when(userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(
                any(),
                any(UserStatus.class)
        )).thenReturn(List.of(recipient));
        Conversation existingConversation = new Conversation();
        existingConversation.setConversationId(UUID.randomUUID());
        existingConversation.setConversationType(ConversationType.DIRECT);
        existingConversation.getParticipants().add(currentUser);
        existingConversation.getParticipants().add(recipient);
        when(conversationRepository.findDirectConversation(
                currentUser.getUserId(),
                recipient.getUserId()
        )).thenReturn(Optional.of(existingConversation));

        ConversationResponse response = conversationService.createConversation(request, userDetails);

        assertEquals(existingConversation.getConversationId(), response.getConversationId());
        assertEquals(recipient.getFullName(), response.getTitle());
        assertEquals(false, response.getIsNew());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createConversationWithUnknownOrInactiveRecipientThrowsNotFound() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT,
                null,
                List.of(recipient.getUserId()),
                null
        );
        when(userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(
                any(),
                any(UserStatus.class)
        )).thenReturn(List.of());

        AppException exception = assertThrows(
                AppException.class,
                () -> conversationService.createConversation(request, userDetails)
        );

        assertEquals(ErrorCode.RECIPIENT_NOT_FOUND, exception.getErrorCode());
        verify(conversationRepository, never()).findDirectConversation(any(), any());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createDirectConversationWithInvalidParticipantCountThrowsValidationError() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT,
                null,
                List.of(recipient.getUserId(), groupParticipant.getUserId()),
                null
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> conversationService.createConversation(request, userDetails)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals(MessageConstant.DIRECT_PARTICIPANT_COUNT_INVALID, exception.getMessage());
        verify(userRepository, never()).findAllByUserIdInAndStatusAndIsDeletedFalse(any(), any());
    }

    @Test
    void createGroupConversationWithFewerThanTwoDistinctParticipantsThrowsValidationError() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.GROUP,
                "Nhóm",
                List.of(recipient.getUserId(), recipient.getUserId()),
                null
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> conversationService.createConversation(request, userDetails)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals(MessageConstant.GROUP_PARTICIPANT_COUNT_INVALID, exception.getMessage());
        verify(userRepository, never()).findAllByUserIdInAndStatusAndIsDeletedFalse(any(), any());
    }

    @Test
    void createGroupConversationWithoutTitleThrowsValidationError() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.GROUP,
                "   ",
                List.of(recipient.getUserId(), groupParticipant.getUserId()),
                null
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> conversationService.createConversation(request, userDetails)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals(MessageConstant.GROUP_TITLE_REQUIRED, exception.getMessage());
        verify(userRepository, never()).findAllByUserIdInAndStatusAndIsDeletedFalse(any(), any());
    }

    private User createUser(String fullName, String avatarUrl) {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setFullName(fullName);
        user.setAvatarUrl(avatarUrl);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsDeleted(false);
        return user;
    }
}
