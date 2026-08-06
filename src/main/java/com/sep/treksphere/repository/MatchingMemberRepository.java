package com.sep.treksphere.repository;

import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.MatchingMember;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.enums.matching.MatchingRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchingMemberRepository extends JpaRepository<MatchingMember, UUID> {

    Optional<MatchingMember> findByMatchingGroupAndUser(MatchingGroup matchingGroup, User user);

    @Query("""
        SELECT mm FROM MatchingMember mm
        JOIN FETCH mm.matchingGroup mg
        JOIN FETCH mg.owner o
        JOIN FETCH mm.user u
        WHERE mm.matchingMemberId = :memberId AND mm.isDeleted = false
    """)
    Optional<MatchingMember> findDetailByMemberId(@Param("memberId") UUID memberId);

    @Query("""
        SELECT mm FROM MatchingMember mm
        JOIN FETCH mm.user u
        WHERE mm.matchingMemberId = :memberId
          AND mm.matchingGroup.matchingGroupId = :groupId
          AND mm.role = :role
          AND mm.isDeleted = false
    """)
    Optional<MatchingMember> findJoinRequestByIdAndGroupId(
            @Param("memberId") UUID memberId,
            @Param("groupId") UUID groupId,
            @Param("role") MatchingRole role
    );

    @Query("""
        SELECT COUNT(mm) FROM MatchingMember mm
        WHERE mm.matchingGroup.matchingGroupId = :groupId
          AND mm.status = :status
          AND mm.isDeleted = false
    """)
    long countActiveMembersByGroupIdAndStatus(
            @Param("groupId") UUID groupId,
            @Param("status") JoinStatus status
    );

    @Query(
        value = """
            SELECT mm FROM MatchingMember mm
            JOIN FETCH mm.user u
            WHERE mm.matchingGroup.matchingGroupId = :groupId
              AND mm.status = :status
              AND mm.role = :role
              AND mm.isDeleted = false
            ORDER BY mm.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(mm) FROM MatchingMember mm
            WHERE mm.matchingGroup.matchingGroupId = :groupId
              AND mm.status = :status
              AND mm.role = :role
              AND mm.isDeleted = false
        """
    )
    Page<MatchingMember> findJoinRequests(
            @Param("groupId") UUID groupId,
            @Param("status") JoinStatus status,
            @Param("role") MatchingRole role,
            Pageable pageable
    );
}
