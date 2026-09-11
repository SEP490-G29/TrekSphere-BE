package com.sep.treksphere.matching.entity;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.TimeSlot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "custom_journey_activity")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class CustomJourneyActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID customJourneyActivityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_journey_id", nullable = false)
    private CustomJourney customJourney;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_journey_checkpoint_id")
    private CustomJourneyCheckpoint checkpoint;

    @Column(nullable = false)
    private Integer dayNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeSlot timeSlot;

    @Column(nullable = false)
    private Integer activityOrder = 1;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String plannedStartAt;

    @Column(length = 50)
    private String plannedEndAt;
}
