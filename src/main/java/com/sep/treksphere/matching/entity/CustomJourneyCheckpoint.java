package com.sep.treksphere.matching.entity;

import com.sep.treksphere.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "custom_journey_checkpoint")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class CustomJourneyCheckpoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID customJourneyCheckpointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_journey_id", nullable = false)
    private CustomJourney customJourney;

    private Integer dayNo;

    @Column(nullable = false)
    private Integer checkpointOrder;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String locationName;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private LocalDateTime plannedStartAt;

    private LocalDateTime plannedEndAt;

    @Column(length = 500)
    private String imageUrl;
}
