package com.sep.treksphere.matching.entity;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "custom_journey")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class CustomJourney extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID customJourneyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id", nullable = false, unique = true)
    private MatchingGroup matchingGroup;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JourneyDifficulty difficulty;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isLocked = false;

    private LocalDateTime lockedAt;

    @OneToMany(mappedBy = "customJourney", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("checkpointOrder ASC")
    private Set<CustomJourneyCheckpoint> checkpoints = new HashSet<>();

    @OneToMany(mappedBy = "customJourney", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNo ASC, timeSlot ASC, activityOrder ASC")
    private Set<CustomJourneyActivity> activities = new HashSet<>();

    @OneToMany(mappedBy = "customJourney", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CustomJourneyCostItem> costItems = new HashSet<>();
}

