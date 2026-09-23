package com.sep.treksphere.notification.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.notification.dto.response.NotificationResponse;
import com.sep.treksphere.notification.entity.Notification;
import com.sep.treksphere.notification.enums.NotificationEventType;
import com.sep.treksphere.notification.enums.ReferenceType;
import com.sep.treksphere.notification.event.NotifyCommand;
import com.sep.treksphere.notification.repository.NotificationRepository;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PaginationResponse<NotificationResponse> list(UUID userId, int page, int size, Boolean isRead) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Page<Notification> notificationPage = isRead == null
                ? notificationRepository.findByRecipient_UserIdAndIsDeletedFalse(
                        userId, PageRequest.of(page - 1, size, sort))
                : notificationRepository.findByRecipient_UserIdAndIsReadAndIsDeletedFalse(
                        userId, isRead, PageRequest.of(page - 1, size, sort));

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

    public void notify(UUID recipientId, NotificationEventType eventType,
                        ReferenceType referenceType, UUID referenceId, String actionUrl,
                        Object... templateArgs) {
        notify(List.of(recipientId), eventType, referenceType, referenceId, actionUrl, templateArgs);
    }

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotification(UUID recipientId, NotifyCommand command) {
        User recipient = userRepository.getReferenceById(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(command.title());
        notification.setEventType(command.eventType());
        notification.setContent(command.content());
        notification.setReferenceType(command.referenceType());
        notification.setReferenceId(command.referenceId());
        notification.setActionUrl(command.actionUrl());
        return notificationRepository.save(notification);
    }
}
