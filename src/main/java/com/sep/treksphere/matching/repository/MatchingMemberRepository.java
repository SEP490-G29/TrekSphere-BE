package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.user.User;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
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

    Optional<MatchingMember> findByMatchingGroupAndUserAndIsDeletedFalse(MatchingGroup matchingGroup, User user);

    boolean existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
            UUID groupId,
            UUID userId,
            JoinStatus status
    );

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
          AND mm.isDeleted = false
    """)
    Optional<MatchingMember> findMemberByIdAndGroupId(
            @Param("memberId") UUID memberId,
            @Param("groupId") UUID groupId
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

    @Query("""
        SELECT mm FROM MatchingMember mm
        JOIN FETCH mm.user u
        WHERE mm.matchingGroup.matchingGroupId = :groupId
          AND mm.status = :status
          AND mm.isDeleted = false
        ORDER BY mm.joinedAt ASC
    """)
    List<MatchingMember> findActiveMembers(
            @Param("groupId") UUID groupId,
            @Param("status") JoinStatus status
    );

    @Query(
        value = """
            SELECT mm FROM MatchingMember mm
            JOIN FETCH mm.matchingGroup mg
            LEFT JOIN FETCH mg.tour t
            LEFT JOIN FETCH mg.customJourney cj
            JOIN FETCH mg.owner o
            WHERE mm.user.userId = :userId
              AND (CAST(:status AS string) IS NULL OR mm.status = :status)
              AND mm.isDeleted = false
              AND mg.isDeleted = false
            ORDER BY mm.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(mm) FROM MatchingMember mm
            JOIN mm.matchingGroup mg
            WHERE mm.user.userId = :userId
              AND (CAST(:status AS string) IS NULL OR mm.status = :status)
              AND mm.isDeleted = false
              AND mg.isDeleted = false
        """
    )
    Page<MatchingMember> findMyMemberships(
            @Param("userId") UUID userId,
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

