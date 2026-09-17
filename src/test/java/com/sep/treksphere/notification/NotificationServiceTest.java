package com.sep.treksphere.notification;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        User recipient = new User();
        recipient.setUserId(userId);
        recipient.setFullName("Trekker User");

        notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setRecipient(recipient);
        notification.setTitle("Test");
        notification.setContent("Test content");
        notification.setEventType(NotificationEventType.SOS_ALERT_RAISED);
        notification.setIsRead(false);
        notification.setIsDeleted(false);
    }

    @Test
    @DisplayName("list - không lọc isRead: truyền Sort createdAt DESC vào Pageable để mới nhất luôn lên đầu")
    void list_NoFilter_SortsByCreatedAtDesc() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(notificationRepository.findByRecipient_UserIdAndIsDeletedFalse(eq(userId), pageableCaptor.capture()))
                .thenReturn(page);

        PaginationResponse<NotificationResponse> response = notificationService.list(userId, 1, 10, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("list - có lọc isRead: vẫn truyền Sort createdAt DESC vào Pageable")
    void list_FilteredByIsRead_SortsByCreatedAtDesc() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(notificationRepository.findByRecipient_UserIdAndIsReadAndIsDeletedFalse(
                        eq(userId), eq(false), pageableCaptor.capture()))
                .thenReturn(page);

        PaginationResponse<NotificationResponse> response = notificationService.list(userId, 1, 10, false);

        assertThat(response.getContent()).hasSize(1);
        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("unreadCount: đếm đúng số thông báo chưa đọc, chưa xoá của user")
    void unreadCount_Success() {
        when(notificationRepository.countByRecipient_UserIdAndIsReadFalseAndIsDeletedFalse(userId))
                .thenReturn(5L);

        assertThat(notificationService.unreadCount(userId)).isEqualTo(5L);
    }

    @Test
    @DisplayName("markAsRead: thông báo thuộc về user -> đánh dấu đã đọc và lưu")
    void markAsRead_Success() {
        UUID notificationId = notification.getNotificationId();
        when(notificationRepository.findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(notificationId, userId))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId, userId);

        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead: không tìm thấy thông báo của user -> ném AppException NOTIFICATION_NOT_FOUND")
    void markAsRead_NotFound_ThrowsException() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(notificationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    @DisplayName("markAllAsRead: uỷ quyền thẳng xuống repository theo userId")
    void markAllAsRead_DelegatesToRepository() {
        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsRead(userId);
    }

    @Test
    @DisplayName("delete: thông báo thuộc về user -> soft-delete và lưu")
    void delete_Success() {
        UUID notificationId = notification.getNotificationId();
        when(notificationRepository.findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(notificationId, userId))
                .thenReturn(Optional.of(notification));

        notificationService.delete(notificationId, userId);

        assertThat(notification.getIsDeleted()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("delete: không tìm thấy thông báo của user -> ném AppException NOTIFICATION_NOT_FOUND")
    void delete_NotFound_ThrowsException() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(notificationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.delete(notificationId, userId))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    @DisplayName("notify (1 người nhận): publish NotifyCommand với danh sách chỉ gồm đúng người đó")
    void notify_SingleRecipient_PublishesEventWithSingletonList() {
        UUID recipientId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();

        notificationService.notify(recipientId, NotificationEventType.USER_STATUS_CHANGED,
                ReferenceType.USER, refId, "/profile", "bị khóa");

        ArgumentCaptor<NotifyCommand> captor = ArgumentCaptor.forClass(NotifyCommand.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().recipientIds()).containsExactly(recipientId);
        assertThat(captor.getValue().referenceId()).isEqualTo(refId);
        assertThat(captor.getValue().actionUrl()).isEqualTo("/profile");
    }

    @Test
    @DisplayName("notify (nhiều người nhận): danh sách rỗng -> không publish event nào")
    void notify_MultiRecipient_EmptyList_DoesNotPublish() {
        notificationService.notify(List.<UUID>of(), NotificationEventType.USER_STATUS_CHANGED,
                ReferenceType.USER, UUID.randomUUID(), "/profile", "bị khóa");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("notify (nhiều người nhận): publish đúng 1 NotifyCommand chứa toàn bộ danh sách")
    void notify_MultiRecipient_PublishesEventWithFullList() {
        UUID recipient1 = UUID.randomUUID();
        UUID recipient2 = UUID.randomUUID();

        notificationService.notify(List.of(recipient1, recipient2), NotificationEventType.USER_STATUS_CHANGED,
                ReferenceType.USER, UUID.randomUUID(), "/profile", "bị khóa");

        ArgumentCaptor<NotifyCommand> captor = ArgumentCaptor.forClass(NotifyCommand.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().recipientIds()).containsExactly(recipient1, recipient2);
    }

    @Test
    @DisplayName("createNotification: tạo và lưu bản ghi thông báo thật từ NotifyCommand (chạy sau khi commit)")
    void createNotification_Success() {
        UUID recipientId = UUID.randomUUID();
        User recipient = new User();
        recipient.setUserId(recipientId);
        NotifyCommand command = new NotifyCommand(
                List.of(recipientId), NotificationEventType.USER_STATUS_CHANGED,
                "Tài khoản của bạn", "Tài khoản của bạn đã bị khóa",
                ReferenceType.USER, recipientId, "/profile");

        when(userRepository.getReferenceById(recipientId)).thenReturn(recipient);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification saved = notificationService.createNotification(recipientId, command);

        assertThat(saved.getRecipient()).isEqualTo(recipient);
        assertThat(saved.getTitle()).isEqualTo("Tài khoản của bạn");
        assertThat(saved.getContent()).isEqualTo("Tài khoản của bạn đã bị khóa");
        assertThat(saved.getReferenceType()).isEqualTo(ReferenceType.USER);
        assertThat(saved.getReferenceId()).isEqualTo(recipientId);
        assertThat(saved.getActionUrl()).isEqualTo("/profile");
    }
}
