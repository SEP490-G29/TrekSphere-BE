package com.sep.treksphere.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `NotificationEventListener` là `@Component` (không phải `@Service`) nên rất dễ bị bỏ sót khỏi
 * bảng theo dõi coverage — bao phủ method duy nhất `onNotify`.
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationEventListener listener;

    private Notification notificationFor(UUID recipientId) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setTitle("Test");
        notification.setContent("Nội dung test");
        notification.setEventType(NotificationEventType.USER_STATUS_CHANGED);
        return notification;
    }

    @Test
    @DisplayName("onNotify: nhiều người nhận -> tạo và đẩy WebSocket cho từng người tới đúng topic")
    void onNotify_MultipleRecipients_CreatesAndBroadcastsForEach() {
        UUID recipient1 = UUID.randomUUID();
        UUID recipient2 = UUID.randomUUID();
        NotifyCommand command = new NotifyCommand(
                List.of(recipient1, recipient2), NotificationEventType.USER_STATUS_CHANGED,
                "Title", "Content", ReferenceType.USER, UUID.randomUUID(), "/profile");

        when(notificationService.createNotification(recipient1, command)).thenReturn(notificationFor(recipient1));
        when(notificationService.createNotification(recipient2, command)).thenReturn(notificationFor(recipient2));

        listener.onNotify(command);

        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + recipient1), any(NotificationResponse.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + recipient2), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("onNotify: 1 recipient lỗi khi tạo notification -> nuốt lỗi, vẫn xử lý tiếp các recipient còn lại")
    void onNotify_OneRecipientFails_ContinuesWithOthers() {
        UUID failingRecipient = UUID.randomUUID();
        UUID okRecipient = UUID.randomUUID();
        NotifyCommand command = new NotifyCommand(
                List.of(failingRecipient, okRecipient), NotificationEventType.USER_STATUS_CHANGED,
                "Title", "Content", ReferenceType.USER, UUID.randomUUID(), "/profile");

        when(notificationService.createNotification(failingRecipient, command))
                .thenThrow(new RuntimeException("DB lỗi"));
        when(notificationService.createNotification(okRecipient, command)).thenReturn(notificationFor(okRecipient));

        assertThatCode(() -> listener.onNotify(command)).doesNotThrowAnyException();

        verify(messagingTemplate, never())
                .convertAndSend(eq("/topic/notifications/" + failingRecipient), any(NotificationResponse.class));
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/notifications/" + okRecipient), any(NotificationResponse.class));
    }
}
