package com.sep.treksphere.matching.entity;


import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "group_peer_review", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_trip_id", "reviewer_matching_member_id", "reviewee_matching_member_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class GroupPeerReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupPeerReviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id", nullable = false)
    private GroupTrip groupTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_matching_member_id", nullable = false)
    private MatchingMember reviewerMatchingMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_matching_member_id", nullable = false)
    private MatchingMember revieweeMatchingMember;

    @Column(nullable = false)
    private Short actualEnduranceRating;

    @Column(nullable = false)
    private Short punctualityResponsibilityRating;

    @Column(nullable = false)
    private Short financialFairnessRating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeerReviewModerationStatus moderationStatus = PeerReviewModerationStatus.VISIBLE;
}
