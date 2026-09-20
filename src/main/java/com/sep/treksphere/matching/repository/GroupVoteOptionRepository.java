package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupVoteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupVoteOptionRepository extends JpaRepository<GroupVoteOption, UUID> {

    @Query("""
        SELECT gvo FROM GroupVoteOption gvo
        LEFT JOIN FETCH gvo.candidateMatchingMember cmm
        LEFT JOIN FETCH cmm.user u
        WHERE gvo.groupVote.groupVoteId = :voteId
          AND gvo.isDeleted = false
        ORDER BY gvo.optionOrder ASC
    """)
    List<GroupVoteOption> findOptionsWithCandidateByVoteId(@Param("voteId") UUID voteId);

    List<GroupVoteOption> findByGroupVote_GroupVoteIdAndIsDeletedFalseOrderByOptionOrderAsc(UUID voteId);

    Optional<GroupVoteOption> findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
            UUID optionId, UUID voteId
    );
}
