package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.response.NotificationResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.Notification;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import com.sep.treksphere.event.NotificationCreatedEvent;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.service.NotificationService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public NotificationResponse create(User recipient, String title, String content,
                                       NotificationEventType eventType, ReferenceType referenceType,
                                       UUID referenceId, String actionUrl) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setEventType(eventType);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setActionUrl(actionUrl);
        NotificationResponse response = toResponse(notificationRepository.save(notification));
        eventPublisher.publishEvent(new NotificationCreatedEvent(recipient.getEmail(), response));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<NotificationResponse> getMyNotifications(
            UUID recipientId, Boolean isRead, Pageable pageable) {
        Page<Notification> page = isRead == null
                ? notificationRepository.findByRecipient_UserIdAndIsDeletedFalse(recipientId, pageable)
                : notificationRepository.findByRecipient_UserIdAndIsReadAndIsDeletedFalse(
                        recipientId, isRead, pageable);
        return PaginationUtils.toPaginationResponse(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID recipientId) {
        return notificationRepository.countByRecipient_UserIdAndIsReadFalseAndIsDeletedFalse(recipientId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID recipientId, UUID notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(notificationId, recipientId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID recipientId) {
        return notificationRepository.markAllRead(recipientId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .eventType(notification.getEventType())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .actionUrl(notification.getActionUrl())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
