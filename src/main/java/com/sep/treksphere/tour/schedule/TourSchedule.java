package com.sep.treksphere.tour.schedule;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.schedule.ScheduleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tour_schedule")
@Getter
@Setter
@NoArgsConstructor


public class TourSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tourScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    private LocalDateTime cancelledAt;
}
