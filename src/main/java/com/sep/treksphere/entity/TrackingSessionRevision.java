package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracking_session_revision")
@Getter
@Setter
@NoArgsConstructor
public class TrackingSessionRevision {

    @Id
    private UUID tourSessionId;

    @Column(nullable = false)
    private Long revision = 0L;

    private UUID lastEventId;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
