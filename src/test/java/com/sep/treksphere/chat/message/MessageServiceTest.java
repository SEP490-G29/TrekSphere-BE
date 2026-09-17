package com.sep.treksphere.chat.message;

import com.sep.treksphere.chat.Conversation;
import com.sep.treksphere.chat.ConversationRepository;
import com.sep.treksphere.chat.ConversationType;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bao phủ đúng 1 method mà `unit_test_gap_report.md` liệt kê thiếu cho `MessageService`:
 * `sendText`. `sendFile`/`getAttachmentUrl` bị loại vì 🚫 Không cần test (FE dùng route upload
 * chung + gửi text message thay vì route file-message riêng của Chat).
 *
 * `sendText` đăng ký `TransactionSynchronization` (để broadcast WebSocket sau khi commit) — cần
 * chủ động bật `TransactionSynchronizationManager` trong test, nếu không sẽ ném
 * `IllegalStateException: Transaction synchronization is not active`.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private FileService fileService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private User otherParticipant;
    private CustomUserDetails userDetails;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();

        sender = new User();
        sender.setUserId(UUID.randomUUID());
        sender.setFullName("Sender");

        otherParticipant = new User();
        otherParticipant.setUserId(UUID.randomUUID());
        otherParticipant.setFullName("Other");

        userDetails = new CustomUserDetails(sender);

        conversation = new Conversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setConversationType(ConversationType.DIRECT);
        conversation.getParticipants().add(sender);
        conversation.getParticipants().add(otherParticipant);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    @DisplayName("sendText: user là participant -> lưu tin nhắn TEXT, broadcast sau commit và báo cho người còn lại")
    void sendText_Success() {
        MessageCreateRequest request = new MessageCreateRequest();
        request.setConversationId(conversation.getConversationId());
        request.setContent("  Xin chào  ");

        when(conversationRepository.findActiveConversationByIdAndParticipantId(
                conversation.getConversationId(), sender.getUserId())).thenReturn(java.util.Optional.of(conversation));
        when(messageRepository.saveAndFlush(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setMessageId(UUID.randomUUID());
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });

        MessageResponse response = messageService.sendText(request, userDetails);

        assertThat(response.getContent()).isEqualTo("Xin chào");
        assertThat(response.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(response.getSenderId()).isEqualTo(sender.getUserId());
        verify(conversationRepository).save(conversation);
        verify(notificationService).notify(
                eq(java.util.List.of(otherParticipant.getUserId())), any(), any(),
                eq(conversation.getConversationId()), eq("/chat"), eq("Sender"), eq("Xin chào"));
    }

    @Test
    @DisplayName("sendText: user không phải participant của hội thoại -> ném AppException CONVERSATION_NOT_FOUND")
    void sendText_NotParticipant_ThrowsException() {
        MessageCreateRequest request = new MessageCreateRequest();
        request.setConversationId(conversation.getConversationId());
        request.setContent("Xin chào");

        when(conversationRepository.findActiveConversationByIdAndParticipantId(
                conversation.getConversationId(), sender.getUserId())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> messageService.sendText(request, userDetails))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND));
    }
}
