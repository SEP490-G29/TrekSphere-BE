package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.MatchingGroup;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "custom_journey")
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

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isLocked = false;

    private LocalDateTime lockedAt;
}
