package com.sep.treksphere.chat;

import com.sep.treksphere.chat.message.MessageRepository;
import com.sep.treksphere.chat.message.MessageResponse;
import com.sep.treksphere.chat.message.MessageService;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import com.sep.treksphere.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bao phủ 10 method mà `unit_test_gap_report.md` liệt kê thiếu cho `ConversationService` (loại
 * `createVendorConversation` vì 🚫 Không cần test — FE dùng check + create generic thay vì gọi
 * endpoint riêng này).
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private MatchingGroupRepository matchingGroupRepository;
    @Mock private NotificationService notificationService;
    @Mock private MessageService messageService;
    @Mock private VendorRepository vendorRepository;

    @InjectMocks
    private ConversationService conversationService;

    private User currentUser;
    private User otherUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setFullName("Current User");

        otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setFullName("Other User");

        userDetails = new CustomUserDetails(currentUser);
    }

    private Conversation directConversation() {
        Conversation conversation = new Conversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setConversationType(ConversationType.DIRECT);
        conversation.setIsDeleted(false);
        conversation.getParticipants().add(currentUser);
        conversation.getParticipants().add(otherUser);
        return conversation;
    }

    private Conversation groupConversation(UUID creatorId) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setConversationType(ConversationType.GROUP);
        conversation.setTitle("Nhóm Fansipan");
        conversation.setIsDeleted(false);
        conversation.setCreatedBy(creatorId.toString());
        conversation.getParticipants().add(currentUser);
        return conversation;
    }

    // ---------------------------------------------------------------
    // getConversations
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getConversations: trả về danh sách hội thoại đang hoạt động của user")
    void getConversations_Success() {
        Conversation conversation = directConversation();
        when(conversationRepository.findActiveConversationsByUserId(eq(currentUser.getUserId()), any()))
                .thenReturn(new PageImpl<>(List.of(conversation)));

        var result = conversationService.getConversations(1, 10, userDetails);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Other User");
    }

    // ---------------------------------------------------------------
    // createConversation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createConversation: DIRECT chưa tồn tại -> tạo mới hội thoại")
    void createConversation_DirectNew_Success() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT, null, List.of(otherUser.getUserId()), null);
        when(userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(any(), eq(UserStatus.ACTIVE)))
                .thenReturn(List.of(otherUser));
        when(conversationRepository.findDirectConversation(currentUser.getUserId(), otherUser.getUserId()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setConversationId(UUID.randomUUID());
            return c;
        });

        ConversationResponse response = conversationService.createConversation(request, userDetails);

        assertThat(response.getConversationType()).isEqualTo(ConversationType.DIRECT);
        assertThat(response.getTitle()).isEqualTo("Other User");
    }

    @Test
    @DisplayName("createConversation: DIRECT đã tồn tại -> trả về hội thoại cũ, không tạo mới")
    void createConversation_DirectExisting_ReturnsExisting() {
        Conversation existing = directConversation();
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT, null, List.of(otherUser.getUserId()), null);
        when(userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(any(), eq(UserStatus.ACTIVE)))
                .thenReturn(List.of(otherUser));
        when(conversationRepository.findDirectConversation(currentUser.getUserId(), otherUser.getUserId()))
                .thenReturn(Optional.of(existing));

        ConversationResponse response = conversationService.createConversation(request, userDetails);

        assertThat(response.getConversationId()).isEqualTo(existing.getConversationId());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createConversation: tự chat với chính mình -> ném AppException CANNOT_CHAT_WITH_SELF")
    void createConversation_SelfChat_ThrowsException() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT, null, List.of(currentUser.getUserId()), null);

        assertThatThrownBy(() -> conversationService.createConversation(request, userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CANNOT_CHAT_WITH_SELF));
    }

    @Test
    @DisplayName("createConversation: người tham gia không tồn tại/không active -> ném AppException RECIPIENT_NOT_FOUND")
    void createConversation_ParticipantNotFound_ThrowsException() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT, null, List.of(otherUser.getUserId()), null);
        when(userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(any(), eq(UserStatus.ACTIVE)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> conversationService.createConversation(request, userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RECIPIENT_NOT_FOUND));
    }

    @Test
    @DisplayName("createConversation: GROUP thiếu title -> ném AppException VALIDATION_ERROR")
    void createConversation_GroupWithoutTitle_ThrowsException() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.GROUP, "  ", List.of(otherUser.getUserId(), UUID.randomUUID()), null);

        assertThatThrownBy(() -> conversationService.createConversation(request, userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    // ---------------------------------------------------------------
    // checkConversation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("checkConversation: DIRECT đã tồn tại -> trả về response, không tạo mới")
    void checkConversation_DirectExisting_ReturnsResponse() {
        Conversation existing = directConversation();
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT, null, List.of(otherUser.getUserId()), null);
        when(conversationRepository.findDirectConversation(currentUser.getUserId(), otherUser.getUserId()))
                .thenReturn(Optional.of(existing));

        ConversationResponse response = conversationService.checkConversation(request, userDetails);

        assertThat(response).isNotNull();
        assertThat(response.getConversationId()).isEqualTo(existing.getConversationId());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkConversation: DIRECT chưa tồn tại -> trả về null")
    void checkConversation_DirectNotExisting_ReturnsNull() {
        ConversationCreateRequest request = new ConversationCreateRequest(
                ConversationType.DIRECT, null, List.of(otherUser.getUserId()), null);
        when(conversationRepository.findDirectConversation(currentUser.getUserId(), otherUser.getUserId()))
                .thenReturn(Optional.empty());

        assertThat(conversationService.checkConversation(request, userDetails)).isNull();
    }

    // ---------------------------------------------------------------
    // getMessages
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getMessages: user là participant -> trả về danh sách tin nhắn đã phân trang")
    void getMessages_Success() {
        Conversation conversation = directConversation();
        com.sep.treksphere.chat.message.Message message = new com.sep.treksphere.chat.message.Message();
        when(conversationRepository.findActiveConversationByIdAndParticipantId(
                conversation.getConversationId(), currentUser.getUserId())).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(message)));
        when(messageService.toResponse(message)).thenReturn(MessageResponse.builder().build());

        var result = conversationService.getMessages(conversation.getConversationId(), 1, 20, userDetails);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getMessages: user không phải participant -> ném AppException CONVERSATION_NOT_FOUND")
    void getMessages_NotParticipant_ThrowsException() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findActiveConversationByIdAndParticipantId(conversationId, currentUser.getUserId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getMessages(conversationId, 1, 20, userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // sendMessage
    // ---------------------------------------------------------------

    @Test
    @DisplayName("sendMessage: uỷ quyền thẳng xuống MessageService.sendText")
    void sendMessage_DelegatesToMessageService() {
        var request = new com.sep.treksphere.chat.message.MessageCreateRequest();
        MessageResponse expected = MessageResponse.builder().build();
        when(messageService.sendText(request, userDetails)).thenReturn(expected);

        MessageResponse response = conversationService.sendMessage(request, userDetails);

        assertThat(response).isEqualTo(expected);
    }

    // ---------------------------------------------------------------
    // markMessagesAsRead
    // ---------------------------------------------------------------

    @Test
    @DisplayName("markMessagesAsRead: user là participant -> đánh dấu toàn bộ tin nhắn đã đọc")
    void markMessagesAsRead_Success() {
        Conversation conversation = directConversation();
        when(conversationRepository.findActiveConversationByIdAndParticipantId(
                conversation.getConversationId(), currentUser.getUserId())).thenReturn(Optional.of(conversation));

        conversationService.markMessagesAsRead(conversation.getConversationId(), userDetails);

        verify(messageRepository).markMessagesAsRead(conversation.getConversationId(), currentUser.getUserId());
    }

    @Test
    @DisplayName("markMessagesAsRead: user không phải participant -> ném AppException CONVERSATION_NOT_FOUND")
    void markMessagesAsRead_NotParticipant_ThrowsException() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findActiveConversationByIdAndParticipantId(conversationId, currentUser.getUserId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.markMessagesAsRead(conversationId, userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // deleteConversation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deleteConversation: DIRECT, đúng participant -> soft-delete thành công")
    void deleteConversation_DirectAsParticipant_Success() {
        Conversation conversation = directConversation();
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        conversationService.deleteConversation(conversation.getConversationId(), userDetails);

        assertThat(conversation.getIsDeleted()).isTrue();
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("deleteConversation: DIRECT, không phải participant -> ném AppException ACCESS_DENIED")
    void deleteConversation_DirectNotParticipant_ThrowsAccessDenied() {
        Conversation conversation = directConversation();
        conversation.getParticipants().remove(currentUser);
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.deleteConversation(conversation.getConversationId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("deleteConversation: GROUP, không phải người tạo -> ném AppException ACCESS_DENIED")
    void deleteConversation_GroupNotCreator_ThrowsAccessDenied() {
        Conversation conversation = groupConversation(UUID.randomUUID());
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.deleteConversation(conversation.getConversationId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("deleteConversation: đã bị xoá từ trước -> ném AppException CONVERSATION_NOT_FOUND")
    void deleteConversation_AlreadyDeleted_ThrowsException() {
        Conversation conversation = directConversation();
        conversation.setIsDeleted(true);
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.deleteConversation(conversation.getConversationId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // removeMember
    // ---------------------------------------------------------------

    @Test
    @DisplayName("removeMember: là trưởng nhóm -> xoá thành viên thành công")
    void removeMember_AsLeader_Success() {
        Conversation conversation = groupConversation(currentUser.getUserId());
        conversation.getParticipants().add(otherUser);
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        conversationService.removeMember(conversation.getConversationId(), otherUser.getUserId(), userDetails);

        assertThat(conversation.getParticipants()).doesNotContain(otherUser);
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("removeMember: không phải trưởng nhóm -> ném AppException ACCESS_DENIED")
    void removeMember_NotLeader_ThrowsAccessDenied() {
        Conversation conversation = groupConversation(UUID.randomUUID());
        conversation.getParticipants().add(otherUser);
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.removeMember(
                conversation.getConversationId(), otherUser.getUserId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    @DisplayName("removeMember: tự xoá bản thân -> ném AppException VALIDATION_ERROR")
    void removeMember_RemoveSelf_ThrowsException() {
        Conversation conversation = groupConversation(currentUser.getUserId());
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.removeMember(
                conversation.getConversationId(), currentUser.getUserId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    @DisplayName("removeMember: hội thoại DIRECT -> ném AppException VALIDATION_ERROR")
    void removeMember_DirectType_ThrowsException() {
        Conversation conversation = directConversation();
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.removeMember(
                conversation.getConversationId(), otherUser.getUserId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    // ---------------------------------------------------------------
    // addMember
    // ---------------------------------------------------------------

    @Test
    @DisplayName("addMember: là trưởng nhóm, thành viên mới hợp lệ -> thêm thành công và gửi thông báo")
    void addMember_AsLeader_Success() {
        Conversation conversation = groupConversation(currentUser.getUserId());
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));
        when(userRepository.findById(otherUser.getUserId())).thenReturn(Optional.of(otherUser));

        conversationService.addMember(conversation.getConversationId(), otherUser.getUserId(), userDetails);

        assertThat(conversation.getParticipants()).contains(otherUser);
        verify(notificationService).notify(
                eq(otherUser.getUserId()), any(), any(), eq(conversation.getConversationId()), eq("/chat"),
                eq(currentUser.getFullName()), eq(conversation.getTitle()));
    }

    @Test
    @DisplayName("addMember: thành viên đã ở trong nhóm -> ném AppException VALIDATION_ERROR")
    void addMember_AlreadyInChat_ThrowsException() {
        Conversation conversation = groupConversation(currentUser.getUserId());
        conversation.getParticipants().add(otherUser);
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.addMember(
                conversation.getConversationId(), otherUser.getUserId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    @DisplayName("addMember: không phải trưởng nhóm -> ném AppException ACCESS_DENIED")
    void addMember_NotLeader_ThrowsException() {
        Conversation conversation = groupConversation(UUID.randomUUID());
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.addMember(
                conversation.getConversationId(), otherUser.getUserId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    // ---------------------------------------------------------------
    // getConversationMembers
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getConversationMembers: user là participant -> trả về danh sách thành viên")
    void getConversationMembers_AsParticipant_Success() {
        Conversation conversation = directConversation();
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        var members = conversationService.getConversationMembers(conversation.getConversationId(), userDetails);

        assertThat(members).hasSize(2);
    }

    @Test
    @DisplayName("getConversationMembers: user không phải participant -> ném AppException ACCESS_DENIED")
    void getConversationMembers_NotParticipant_ThrowsException() {
        Conversation conversation = directConversation();
        conversation.getParticipants().remove(currentUser);
        when(conversationRepository.findById(conversation.getConversationId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.getConversationMembers(conversation.getConversationId(), userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }
}
