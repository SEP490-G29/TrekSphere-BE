package com.sep.treksphere.matching.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_vote_ballot", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_vote_id", "voter_matching_member_id"})
})
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GroupVoteBallot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupVoteBallotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_vote_id", nullable = false)
    private GroupVote groupVote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_vote_option_id", nullable = false)
    private GroupVoteOption groupVoteOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_matching_member_id", nullable = false)
    private MatchingMember voterMatchingMember;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
