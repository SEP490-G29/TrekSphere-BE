package com.sep.treksphere.matching.member;

import com.sep.treksphere.matching.MatchingGroup;
import com.sep.treksphere.matching.member.MatchingMember;
import com.sep.treksphere.user.User;
import com.sep.treksphere.matching.JoinStatus;
import com.sep.treksphere.matching.MatchingRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

    @Query(
        value = """
            SELECT mm FROM MatchingMember mm
            JOIN FETCH mm.matchingGroup mg
            JOIN FETCH mg.tour t
            JOIN FETCH mg.owner o
            WHERE mm.user.userId = :userId
              AND mm.role = :role
              AND (CAST(:status AS string) IS NULL OR mm.status = :status)
              AND mm.isDeleted = false
              AND mg.isDeleted = false
        """,
        countQuery = """
            SELECT COUNT(mm) FROM MatchingMember mm
            JOIN mm.matchingGroup mg
            JOIN mg.tour t
            WHERE mm.user.userId = :userId
              AND mm.role = :role
              AND (CAST(:status AS string) IS NULL OR mm.status = :status)
              AND mm.isDeleted = false
              AND mg.isDeleted = false
        """
    )
    Page<MatchingMember> findMyJoinRequests(
            @Param("userId") UUID userId,
            @Param("role") MatchingRole role,
            @Param("status") JoinStatus status,
            Pageable pageable
    );

    /**
     * MatchingGroup không có FK trực tiếp tới TourSchedule (chỉ có tour + targetDate),
     * nên nhóm nào "gắn" với 1 lịch khởi hành được xác định gián tiếp qua cặp (tour, targetDate = departureDate).
     */
    @Query("""
        SELECT DISTINCT mm.user.userId FROM MatchingMember mm
        WHERE mm.matchingGroup.tour.tourId = :tourId
          AND mm.matchingGroup.targetDate = :targetDate
          AND mm.status = :status
          AND mm.isDeleted = false
          AND mm.matchingGroup.isDeleted = false
    """)
    List<UUID> findAcceptedMemberUserIdsByTourAndTargetDate(
            @Param("tourId") UUID tourId,
            @Param("targetDate") LocalDate targetDate,
            @Param("status") JoinStatus status
    );
}
