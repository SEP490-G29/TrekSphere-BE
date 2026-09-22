package com.sep.treksphere.tour.entity;

import com.sep.treksphere.tour.enums.TourBehaviorEventType;
import com.sep.treksphere.tour.enums.TourBehaviorSource;
import com.sep.treksphere.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tour_behavior_event")
@Getter
@Setter
@NoArgsConstructor
public class TourBehaviorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "behavior_event_id")
    private UUID behaviorEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private TourBehaviorEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TourBehaviorSource source;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "display_position")
    private Integer displayPosition;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void setOccurredAtIfMissing() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
