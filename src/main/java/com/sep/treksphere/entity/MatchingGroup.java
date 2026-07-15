package com.sep.treksphere.entity;

import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "matching_group")
@Getter
@Setter
@NoArgsConstructor


public class MatchingGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID matchingGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String groupName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer maxSize;

    @Column(nullable = false)
    private Integer currentSize = 1;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column( nullable = false)
    private LocalDateTime matchingDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MatchingGroupStatus status = MatchingGroupStatus.OPEN;

    @OneToMany(mappedBy = "matchingGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MatchingMember> members = new HashSet<>();
}
