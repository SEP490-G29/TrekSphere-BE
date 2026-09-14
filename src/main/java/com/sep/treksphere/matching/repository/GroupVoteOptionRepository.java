package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupVoteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupVoteOptionRepository extends JpaRepository<GroupVoteOption, UUID> {

    List<GroupVoteOption> findByGroupVote_GroupVoteIdAndIsDeletedFalseOrderByOptionOrderAsc(UUID voteId);

    Optional<GroupVoteOption> findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
            UUID optionId, UUID voteId
    );
}
