package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupVote;
import com.sep.treksphere.matching.entity.GroupVoteBallot;
import com.sep.treksphere.matching.entity.GroupVoteOption;
import com.sep.treksphere.matching.entity.MatchingMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupVoteBallotRepository extends JpaRepository<GroupVoteBallot, UUID> {

    boolean existsByGroupVoteAndVoterMatchingMember(GroupVote groupVote, MatchingMember voterMatchingMember);

    Optional<GroupVoteBallot> findByGroupVoteAndVoterMatchingMember(
            GroupVote groupVote, MatchingMember voterMatchingMember
    );

    long countByGroupVote(GroupVote groupVote);

    long countByGroupVoteAndGroupVoteOption(GroupVote groupVote, GroupVoteOption groupVoteOption);

    List<GroupVoteBallot> findByGroupVote(GroupVote groupVote);

    /** Dùng ở P4-S5: xoá ballot khi member rời/bị xoá trong lúc vote còn OPEN. */
    void deleteByGroupVoteAndVoterMatchingMember(GroupVote groupVote, MatchingMember voterMatchingMember);
}
