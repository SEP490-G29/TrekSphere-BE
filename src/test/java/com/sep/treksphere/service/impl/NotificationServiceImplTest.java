package com.sep.treksphere.service.impl;

import com.sep.treksphere.entity.Notification;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import com.sep.treksphere.event.NotificationCreatedEvent;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private NotificationServiceImpl service;

    @Test
    void createPersistsAndPublishesRealtimeEvent() {
        User recipient = new User();
        recipient.setUserId(UUID.randomUUID());
        recipient.setEmail("trekker@example.com");
        UUID bookingId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(UUID.randomUUID());
            notification.setCreatedAt(LocalDateTime.now());
            return notification;
        });

        var response = service.create(recipient, "Booking created", "Please pay",
                NotificationEventType.BOOKING_CREATED, ReferenceType.BOOKING,
                bookingId, "/trekker/bookings/" + bookingId);

        assertEquals(bookingId, response.getReferenceId());
        ArgumentCaptor<NotificationCreatedEvent> event =
                ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(recipient.getEmail(), event.getValue().recipientEmail());
        assertEquals(response.getNotificationId(), event.getValue().notification().getNotificationId());
    }

    @Test
    void markAsReadOnlyFindsNotificationOwnedByCurrentUser() {
        UUID recipientId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setNotificationId(notificationId);
        notification.setIsRead(false);
        when(notificationRepository.findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(
                notificationId, recipientId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        var response = service.markAsRead(recipientId, notificationId);

        assertEquals(true, response.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadReturnsNotFoundForAnotherUsersNotification() {
        UUID recipientId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findByNotificationIdAndRecipient_UserIdAndIsDeletedFalse(
                notificationId, recipientId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> service.markAsRead(recipientId, notificationId));

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
    }
}
