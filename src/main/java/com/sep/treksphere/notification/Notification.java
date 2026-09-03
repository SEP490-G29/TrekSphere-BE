package com.sep.treksphere.notification;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.user.User;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.ReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor


public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ReferenceType referenceType;

    private UUID referenceId;

    @Column(length = 500)
    private String actionUrl;

    @Column(nullable = false)
    private Boolean isRead = false;
}
