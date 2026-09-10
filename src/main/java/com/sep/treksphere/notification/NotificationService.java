package com.sep.treksphere.notification;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PaginationResponse<NotificationResponse> list(UUID userId, int page, int size, Boolean isRead) {
        Page<Notification> notificationPage = isRead == null
                ? notificationRepository.findByRecipient_UserIdAndIsDeletedFalse(
                        userId, PageRequest.of(page - 1, size))
                : notificationRepository.findByRecipient_UserIdAndIsReadAndIsDeletedFalse(
                        userId, isRead, PageRequest.of(page - 1, size));

        return PaginationResponse.<NotificationResponse>builder()
                .content(notificationPage.getContent().stream().map(NotificationResponse::from).toList())
                .pageNumber(notificationPage.getNumber() + 1)
                .pageSize(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByRecipient_UserIdAndIsReadFalseAndIsDeletedFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = findOwned(notificationId, userId);
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void delete(UUID notificationId, UUID userId) {
        Notification notification = findOwned(notificationId, userId);
        notification.setIsDeleted(true);
        notificationRepository.save(notification);
    }

    private Notification findOwned(UUID notificationId, UUID userId) {
        return notificationRepository
                .findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    /**
     * 1 người nhận — ví dụ: báo cho chủ nhóm, báo cho tác giả blog, báo cho vendor manager.
     * Chỉ cần gọi hàm này tại nơi nghiệp vụ xảy ra; phần lưu DB + đẩy WebSocket do
     * {@link NotificationEventListener} xử lý sau khi transaction hiện tại commit.
     */
    public void notify(UUID recipientId, NotificationEventType eventType,
                        ReferenceType referenceType, UUID referenceId, String actionUrl,
                        Object... templateArgs) {
        notify(List.of(recipientId), eventType, referenceType, referenceId, actionUrl, templateArgs);
    }

    /** Nhiều người nhận (fan-out) — ví dụ: báo cho toàn bộ Admin, toàn bộ thành viên 1 nhóm. */
    public void notify(List<UUID> recipientIds, NotificationEventType eventType,
                        ReferenceType referenceType, UUID referenceId, String actionUrl,
                        Object... templateArgs) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        NotificationTemplates.Resolved resolved = NotificationTemplates.resolve(eventType, templateArgs);
        eventPublisher.publishEvent(new NotifyCommand(
                recipientIds, eventType, resolved.title(), resolved.content(),
                referenceType, referenceId, actionUrl));
    }
}
