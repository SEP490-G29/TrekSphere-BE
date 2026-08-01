package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.NotificationCreateCommand;
import com.sep.treksphere.dto.response.NotificationResponse;
import com.sep.treksphere.dto.response.NotificationUnreadCountResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.Notification;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.user.UserStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.NotificationService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<NotificationResponse> getMyNotifications(
            int page,
            int size,
            Boolean isRead,
            CustomUserDetails userDetails
    ) {
        UUID recipientId = userDetails.getUser().getUserId();
        int normalizedPage = Math.max(page, 1) - 1;
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<NotificationResponse> notifications = notificationRepository
                .findInbox(recipientId, isRead, PageRequest.of(normalizedPage, normalizedSize))
                .map(this::toResponse);

        return PaginationUtils.toPaginationResponse(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(CustomUserDetails userDetails) {
        UUID recipientId = userDetails.getUser().getUserId();
        return unreadCountResponse(recipientId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, CustomUserDetails userDetails) {
        UUID recipientId = userDetails.getUser().getUserId();
        Notification notification = findOwnedNotification(notificationId, recipientId);

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
            broadcastUnreadCountAfterCommit(recipientId);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(CustomUserDetails userDetails) {
        UUID recipientId = userDetails.getUser().getUserId();
        notificationRepository.markAllAsRead(recipientId);
        broadcastUnreadCountAfterCommit(recipientId);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId, CustomUserDetails userDetails) {
        UUID recipientId = userDetails.getUser().getUserId();
        Notification notification = findOwnedNotification(notificationId, recipientId);

        notification.setIsDeleted(true);
        notification.setDeletedAt(LocalDateTime.now());
        notification.setDeletedBy(userDetails.getUsername());
        notificationRepository.save(notification);
        broadcastUnreadCountAfterCommit(recipientId);
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationCreateCommand command) {
        User recipient = userRepository
                .findByUserIdAndStatusAndIsDeletedFalse(command.getRecipientId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notification savedNotification = notificationRepository.save(toEntity(command, recipient));
        NotificationResponse response = toResponse(savedNotification);
        broadcastNotificationAfterCommit(recipient.getUserId(), response);
        return response;
    }

    @Override
    @Transactional
    public List<NotificationResponse> createNotifications(List<NotificationCreateCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, User> recipients = loadRecipients(commands);
        List<Notification> notifications = commands.stream()
                .map(command -> toEntity(command, recipients.get(command.getRecipientId())))
                .toList();
        List<NotificationResponse> responses = notificationRepository.saveAll(notifications).stream()
                .map(this::toResponse)
                .toList();

        for (int index = 0; index < responses.size(); index++) {
            broadcastNotificationAfterCommit(commands.get(index).getRecipientId(), responses.get(index));
        }
        return responses;
    }

    @Override
    @Transactional
    public List<NotificationResponse> createAdminNotifications(NotificationCreateCommand template) {
        List<User> admins = userRepository.findActiveUsersByRole(ADMIN_ROLE, UserStatus.ACTIVE);
        List<Notification> notifications = admins.stream()
                .map(admin -> toEntity(template, admin))
                .toList();
        List<NotificationResponse> responses = notificationRepository.saveAll(notifications).stream()
                .map(this::toResponse)
                .toList();

        for (int index = 0; index < responses.size(); index++) {
            broadcastNotificationAfterCommit(admins.get(index).getUserId(), responses.get(index));
        }
        return responses;
    }

    private Notification findOwnedNotification(UUID notificationId, UUID recipientId) {
        return notificationRepository
                .findByNotificationIdAndRecipientUserIdAndIsDeletedFalse(notificationId, recipientId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private Map<UUID, User> loadRecipients(Collection<NotificationCreateCommand> commands) {
        List<UUID> recipientIds = commands.stream()
                .map(NotificationCreateCommand::getRecipientId)
                .distinct()
                .toList();
        Map<UUID, User> recipients = new LinkedHashMap<>();
        userRepository.findAllByUserIdInAndStatusAndIsDeletedFalse(recipientIds, UserStatus.ACTIVE)
                .forEach(user -> recipients.put(user.getUserId(), user));

        if (recipients.size() != recipientIds.size()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return recipients;
    }

    private Notification toEntity(NotificationCreateCommand command, User recipient) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(command.getTitle());
        notification.setContent(command.getContent());
        notification.setEventType(command.getEventType());
        notification.setReferenceType(command.getReferenceType());
        notification.setReferenceId(command.getReferenceId());
        notification.setActionUrl(command.getActionUrl());
        notification.setIsRead(false);
        return notification;
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

    private NotificationUnreadCountResponse unreadCountResponse(UUID recipientId) {
        return NotificationUnreadCountResponse.builder()
                .unreadCount(notificationRepository
                        .countByRecipientUserIdAndIsReadFalseAndIsDeletedFalse(recipientId))
                .build();
    }

    private void broadcastNotificationAfterCommit(UUID recipientId, NotificationResponse response) {
        runAfterCommit(ignored -> {
            messagingTemplate.convertAndSend(
                    "/topic/users/" + recipientId + "/notifications",
                    response
            );
            broadcastUnreadCount(recipientId);
        });
    }

    private void broadcastUnreadCountAfterCommit(UUID recipientId) {
        runAfterCommit(ignored -> broadcastUnreadCount(recipientId));
    }

    private void broadcastUnreadCount(UUID recipientId) {
        messagingTemplate.convertAndSend(
                "/topic/users/" + recipientId + "/notifications/unread-count",
                unreadCountResponse(recipientId)
        );
    }

    private void runAfterCommit(Consumer<Void> action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.accept(null);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.accept(null);
                } catch (RuntimeException exception) {
                    log.error("Unable to broadcast realtime notification after commit", exception);
                }
            }
        });
    }
}
