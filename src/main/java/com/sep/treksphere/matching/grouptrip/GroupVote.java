package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.MatchingGroup;
import com.sep.treksphere.matching.member.MatchingMember;
import com.sep.treksphere.matching.grouptrip.VoteStatus;
import com.sep.treksphere.matching.grouptrip.VoteType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_vote")
@Getter
@Setter
@NoArgsConstructor
public class GroupVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupVoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id", nullable = false)
    private MatchingGroup matchingGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VoteType voteType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member", nullable = false)
    private MatchingMember createdByMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteStatus status = VoteStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime opensAt;

    @Column(nullable = false)
    private LocalDateTime closesAt;

    @Column(nullable = false)
    private Integer eligibleVoterCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_option_id")
    private GroupVoteOption winningOption;

    private LocalDateTime closedAt;
}
