package com.sep.treksphere.notification.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.notification.dto.response.NotificationResponse;
import com.sep.treksphere.notification.entity.Notification;
import com.sep.treksphere.notification.enums.NotificationEventType;
import com.sep.treksphere.notification.repository.NotificationRepository;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.repository.UserRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
}
