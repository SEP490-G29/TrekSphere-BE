package com.sep.treksphere.entity;

import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "matching_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(nullable = false, length = 255)
    private String groupName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer maxSize;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentSize = 1;

    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MatchingGroupStatus status = MatchingGroupStatus.OPEN;

    @OneToMany(mappedBy = "matchingGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<MatchingMember> members = new HashSet<>();
}
