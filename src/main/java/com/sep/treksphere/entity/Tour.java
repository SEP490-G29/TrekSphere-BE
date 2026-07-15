package com.sep.treksphere.entity;

import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.enums.tour.TourStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tour")
@Getter
@Setter
@NoArgsConstructor


public class Tour extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tourId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, length = 255)
    private String tourName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DifficultyLevel difficulty;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private Integer minCapacity = 1;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(precision = 5, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Column(columnDefinition = "TEXT")
    private String includes;

    @Column(columnDefinition = "TEXT")
    private String excludes;

    @Column(length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TourStatus status = TourStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
}
