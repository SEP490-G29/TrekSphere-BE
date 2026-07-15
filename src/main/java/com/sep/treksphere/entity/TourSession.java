package com.sep.treksphere.entity;

import com.sep.treksphere.enums.tour.TourSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tour_session")
@Getter
@Setter
@NoArgsConstructor
public class TourSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tourSessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_schedule_id", unique = true)
    private TourSchedule tourSchedule;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TourSessionStatus status = TourSessionStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
