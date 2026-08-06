package com.sep.treksphere.repository;

import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import com.sep.treksphere.enums.matching.MatchingRole;
import com.sep.treksphere.enums.tour.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchingGroupRepository extends JpaRepository<MatchingGroup, UUID> {

    boolean existsByOwnerAndStatusInAndTargetDateGreaterThanEqualAndIsDeletedFalse(
            User owner,
            Collection<MatchingGroupStatus> statuses,
            LocalDate targetDate
    );

    @Query(value = """
        SELECT mg FROM MatchingGroup mg
        JOIN FETCH mg.tour t
        JOIN FETCH mg.owner o
        WHERE (
              o.userId = :userId
              OR EXISTS (
                  SELECT 1 FROM MatchingMember mm
                  WHERE mm.matchingGroup = mg
                    AND mm.user.userId = :userId
                    AND mm.role = :memberRole
                    AND mm.status = :acceptedStatus
              )
          )
          AND (:status IS NULL OR mg.status = :status)
          AND (
              :keyword = ''
              OR LOWER(mg.groupName) LIKE CONCAT('%', :keyword, '%')
              OR LOWER(t.tourName) LIKE CONCAT('%', :keyword, '%')
          )
    """, countQuery = """
        SELECT COUNT(mg) FROM MatchingGroup mg
        JOIN mg.tour t
        WHERE (
              mg.owner.userId = :userId
              OR EXISTS (
                  SELECT 1 FROM MatchingMember mm
                  WHERE mm.matchingGroup = mg
                    AND mm.user.userId = :userId
                    AND mm.role = :memberRole
                    AND mm.status = :acceptedStatus
              )
          )
          AND (:status IS NULL OR mg.status = :status)
          AND (
              :keyword = ''
              OR LOWER(mg.groupName) LIKE CONCAT('%', :keyword, '%')
              OR LOWER(t.tourName) LIKE CONCAT('%', :keyword, '%')
          )
    """)
    Page<MatchingGroup> findOwnedOrJoinedGroups(
            @Param("userId") UUID userId,
            @Param("memberRole") MatchingRole memberRole,
            @Param("acceptedStatus") JoinStatus acceptedStatus,
            @Param("status") MatchingGroupStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(value = """
        SELECT mg FROM MatchingGroup mg
        JOIN FETCH mg.tour t
        JOIN FETCH mg.owner o
        WHERE mg.isDeleted = false
          AND mg.status = :status
          AND mg.currentSize < mg.maxSize
          AND mg.matchingDeadline > :now
          AND mg.targetDate > :today
          AND t.isDeleted = false
          AND t.status = :tourStatus
          AND (:tourId IS NULL OR t.tourId = :tourId)
          AND (:targetDate IS NULL OR mg.targetDate = :targetDate)
          AND (
              :keyword = ''
              OR LOWER(mg.groupName) LIKE CONCAT('%', :keyword, '%')
              OR LOWER(t.tourName) LIKE CONCAT('%', :keyword, '%')
          )
        """, countQuery = """
        SELECT COUNT(mg) FROM MatchingGroup mg
        JOIN mg.tour t
        WHERE mg.isDeleted = false
          AND mg.status = :status
          AND mg.currentSize < mg.maxSize
          AND mg.matchingDeadline > :now
          AND mg.targetDate > :today
          AND t.isDeleted = false
          AND t.status = :tourStatus
          AND (:tourId IS NULL OR t.tourId = :tourId)
          AND (:targetDate IS NULL OR mg.targetDate = :targetDate)
          AND (
              :keyword = ''
              OR LOWER(mg.groupName) LIKE CONCAT('%', :keyword, '%')
              OR LOWER(t.tourName) LIKE CONCAT('%', :keyword, '%')
          )
        """)
    Page<MatchingGroup> findAvailableMatchingGroups(
            @Param("status") MatchingGroupStatus status,
            @Param("tourStatus") TourStatus tourStatus,
            @Param("tourId") UUID tourId,
            @Param("targetDate") LocalDate targetDate,
            @Param("keyword") String keyword,
            @Param("today") LocalDate today,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT mg FROM MatchingGroup mg
        JOIN FETCH mg.tour t
        JOIN FETCH mg.owner o
        LEFT JOIN FETCH mg.members m
        LEFT JOIN FETCH m.user mu
        WHERE mg.matchingGroupId = :id
          AND mg.isDeleted = false
          AND mg.status IN :statuses
          AND t.isDeleted = false
          AND t.status = :tourStatus
    """)
    Optional<MatchingGroup> findPublicDetailById(
            @Param("id") UUID id,
            @Param("statuses") Collection<MatchingGroupStatus> statuses,
            @Param("tourStatus") TourStatus tourStatus
    );

    @Query("""
        SELECT mg FROM MatchingGroup mg
        JOIN FETCH mg.tour t
        JOIN FETCH mg.owner o
        LEFT JOIN FETCH mg.members m
        LEFT JOIN FETCH m.user mu
        WHERE mg.matchingGroupId = :id
          AND mg.isDeleted = false
    """)
    Optional<MatchingGroup> findDetailById(@Param("id") UUID id);

    @Query("""
        SELECT mg FROM MatchingGroup mg
        JOIN FETCH mg.owner o
        WHERE mg.matchingGroupId = :id
          AND mg.isDeleted = false
    """)
    Optional<MatchingGroup> findWithOwnerById(@Param("id") UUID id);
}

