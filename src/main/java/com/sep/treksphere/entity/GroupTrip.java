package com.sep.treksphere.entity;

import com.sep.treksphere.enums.group.GroupTripStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_trip")
@Getter
@Setter
@NoArgsConstructor
public class GroupTrip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupTripId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id", nullable = false, unique = true)
    private MatchingGroup matchingGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupTripStatus status = GroupTripStatus.PLANNED;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_by")
    private MatchingMember startBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ended_by")
    private MatchingMember endedBy;
}
