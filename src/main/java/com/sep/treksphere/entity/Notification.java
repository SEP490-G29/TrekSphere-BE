package com.sep.treksphere.entity;

import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ReferenceType referenceType;

    private UUID referenceID;

    @Column(nullable = false)
    private Boolean isRead = false;
}
