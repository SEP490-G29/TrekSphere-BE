package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupVote;
import com.sep.treksphere.matching.enums.VoteStatus;
import com.sep.treksphere.matching.enums.VoteType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupVoteRepository extends JpaRepository<GroupVote, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT v FROM GroupVote v
        JOIN FETCH v.matchingGroup mg
        JOIN FETCH v.createdByMember cbm
        JOIN FETCH cbm.user cbmu
        WHERE v.groupVoteId = :voteId AND v.isDeleted = false
    """)
    Optional<GroupVote> findByIdForUpdate(@Param("voteId") UUID voteId);

    @Query("""
        SELECT v FROM GroupVote v
        WHERE v.matchingGroup.matchingGroupId = :groupId
          AND v.groupVoteId = :voteId
          AND v.isDeleted = false
    """)
    Optional<GroupVote> findByGroupIdAndVoteId(@Param("groupId") UUID groupId, @Param("voteId") UUID voteId);

    @Query("""
        SELECT v FROM GroupVote v
        WHERE v.matchingGroup.matchingGroupId = :groupId
          AND (CAST(:voteType AS string) IS NULL OR v.voteType = :voteType)
          AND (CAST(:status AS string) IS NULL OR v.status = :status)
          AND v.isDeleted = false
        ORDER BY v.createdAt DESC
    """)
    Page<GroupVote> findByGroupWithFilters(
            @Param("groupId") UUID groupId,
            @Param("voteType") VoteType voteType,
            @Param("status") VoteStatus status,
            Pageable pageable
    );

    boolean existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
            UUID groupId, VoteType voteType, VoteStatus status
    );
}
