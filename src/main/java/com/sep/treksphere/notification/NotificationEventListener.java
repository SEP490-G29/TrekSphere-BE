package com.sep.treksphere.notification;

import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Nghe {@link NotifyCommand} sau khi transaction gốc commit thành công: lưu 1 bản ghi Notification
 * cho mỗi recipient rồi đẩy real-time qua STOMP topic "/topic/notifications/{recipientId}".
 * Service nghiệp vụ chỉ cần gọi NotificationService.notify(...) — không cần biết gì về phần này.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotify(NotifyCommand command) {
        for (UUID recipientId : command.recipientIds()) {
            try {
                User recipient = userRepository.getReferenceById(recipientId);

                Notification notification = new Notification();
                notification.setRecipient(recipient);
                notification.setTitle(command.title());
                notification.setEventType(command.eventType());
                notification.setContent(command.content());
                notification.setReferenceType(command.referenceType());
                notification.setReferenceId(command.referenceId());
                notification.setActionUrl(command.actionUrl());
                notification = notificationRepository.save(notification);

                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + recipientId,
                        NotificationResponse.from(notification));
            } catch (Exception ex) {
                log.error("Không thể tạo/đẩy notification cho recipient {} (event {})",
                        recipientId, command.eventType(), ex);
            }
        }
    }
}
