package com.sep.treksphere.notification.service;

import com.sep.treksphere.notification.dto.response.NotificationResponse;
import com.sep.treksphere.notification.entity.Notification;
import com.sep.treksphere.notification.event.NotifyCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotify(NotifyCommand command) {
        for (UUID recipientId : command.recipientIds()) {
            try {
                Notification notification = notificationService.createNotification(recipientId, command);

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
