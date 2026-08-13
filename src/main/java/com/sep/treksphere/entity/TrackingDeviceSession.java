package com.sep.treksphere.entity;

import com.sep.treksphere.enums.tracking.TrackingDeviceSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracking_device_session")
@Getter
@Setter
@NoArgsConstructor
public class TrackingDeviceSession {

    @Id
    private UUID trackingDeviceSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_session_id", nullable = false)
    private TourSession tourSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false)
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrackingDeviceSessionStatus status;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant lastSeenAt;
    private Instant revokedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (trackingDeviceSessionId == null) trackingDeviceSessionId = UUID.randomUUID();
        if (status == null) status = TrackingDeviceSessionStatus.ACTIVE;
        if (issuedAt == null) issuedAt = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
