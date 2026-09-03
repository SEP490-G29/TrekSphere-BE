package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.member.MatchingMember;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "group_vote_option", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_vote_id", "option_order"})
})
@Getter
@Setter
@NoArgsConstructor
public class GroupVoteOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupVoteOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_vote_id", nullable = false)
    private GroupVote groupVote;

    @Column(nullable = false)
    private Integer optionOrder;

    @Column(nullable = false, length = 200)
    private String optionLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_matching_member_id")
    private MatchingMember candidateMatchingMember;
}
