package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    boolean existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
            User owner,
            Tour tour,
            Collection<MatchingGroupStatus> statuses,
            LocalDateTime matchingDeadline,
            LocalDate targetDate
    );

    boolean existsByOwnerAndTourIsNullAndGroupNameIgnoreCaseAndTargetDateAndIsDeletedFalse(
            User owner,
            String groupName,
            LocalDate targetDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT mg FROM MatchingGroup mg
        LEFT JOIN FETCH mg.tour t
        LEFT JOIN FETCH mg.customJourney cj
        JOIN FETCH mg.owner o
        WHERE mg.matchingGroupId = :groupId
          AND mg.isDeleted = false
    """)
    Optional<MatchingGroup> findByIdForUpdate(@Param("groupId") UUID groupId);

    @Query(value = """
        SELECT DISTINCT mg FROM MatchingGroup mg
        LEFT JOIN FETCH mg.tour t
        LEFT JOIN FETCH mg.customJourney cj
        JOIN FETCH mg.owner o
        WHERE (
              (CAST(:role AS string) IS NULL AND (
                  o.userId = :userId
                  OR EXISTS (
                      SELECT 1 FROM MatchingMember mm
                      WHERE mm.matchingGroup = mg
                        AND mm.user.userId = :userId
                        AND mm.role = :memberRole
                        AND mm.status = :acceptedStatus
                        AND mm.isDeleted = false
                  )
              ))
              OR (CAST(:role AS string) = 'LEADER' AND o.userId = :userId)
              OR (CAST(:role AS string) = 'MEMBER' AND EXISTS (
                  SELECT 1 FROM MatchingMember mm
                  WHERE mm.matchingGroup = mg
                    AND mm.user.userId = :userId
                    AND mm.role = :memberRole
                    AND mm.status = :acceptedStatus
                    AND mm.isDeleted = false
              ))
          )
          AND mg.isDeleted = false
          AND (CAST(:status AS string) IS NULL OR mg.status = :status)
          AND (
              CAST(:keyword AS string) = ''
              OR LOWER(mg.groupName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
              OR (t IS NOT NULL AND LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              OR (cj IS NOT NULL AND LOWER(cj.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          )
    """, countQuery = """
        SELECT COUNT(DISTINCT mg) FROM MatchingGroup mg
        LEFT JOIN mg.tour t
        LEFT JOIN mg.customJourney cj
        WHERE (
              (CAST(:role AS string) IS NULL AND (
                  mg.owner.userId = :userId
                  OR EXISTS (
                      SELECT 1 FROM MatchingMember mm
                      WHERE mm.matchingGroup = mg
                        AND mm.user.userId = :userId
                        AND mm.role = :memberRole
                        AND mm.status = :acceptedStatus
                        AND mm.isDeleted = false
                  )
              ))
              OR (CAST(:role AS string) = 'LEADER' AND mg.owner.userId = :userId)
              OR (CAST(:role AS string) = 'MEMBER' AND EXISTS (
                  SELECT 1 FROM MatchingMember mm
                  WHERE mm.matchingGroup = mg
                    AND mm.user.userId = :userId
                    AND mm.role = :memberRole
                    AND mm.status = :acceptedStatus
                    AND mm.isDeleted = false
              ))
          )
          AND mg.isDeleted = false
          AND (CAST(:status AS string) IS NULL OR mg.status = :status)
          AND (
              CAST(:keyword AS string) = ''
              OR LOWER(mg.groupName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
              OR (t IS NOT NULL AND LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              OR (cj IS NOT NULL AND LOWER(cj.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          )
    """)
    Page<MatchingGroup> findOwnedOrJoinedGroups(
            @Param("userId") UUID userId,
            @Param("memberRole") MatchingRole memberRole,
            @Param("acceptedStatus") JoinStatus acceptedStatus,
            @Param("role") MatchingRole role,
            @Param("status") MatchingGroupStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(value = """
        SELECT DISTINCT mg FROM MatchingGroup mg
        LEFT JOIN FETCH mg.tour t
        LEFT JOIN FETCH mg.customJourney cj
        JOIN FETCH mg.owner o
        WHERE mg.isDeleted = false
          AND mg.status = :status
          AND (:availableSlotsOnly = false OR mg.currentSize < mg.maxSize)
          AND mg.matchingDeadline > :now
          AND mg.targetDate > :today
          AND (
              (t IS NOT NULL AND t.isDeleted = false AND t.status = :tourStatus)
              OR (cj IS NOT NULL AND cj.isDeleted = false)
          )
          AND (CAST(:tourId AS uuid) IS NULL OR (t IS NOT NULL AND t.tourId = :tourId))
          AND (
              CAST(:sourceType AS string) IS NULL
              OR (:sourceType = 'TOUR' AND t IS NOT NULL)
              OR (:sourceType = 'CUSTOM_JOURNEY' AND cj IS NOT NULL)
          )
          AND t.isDeleted = false
          AND t.status = :tourStatus
          AND t.vendor.status = com.sep.treksphere.vendor.VendorStatus.ACTIVE
          AND t.vendor.isDeleted = false
          AND (CAST(:tourId AS uuid) IS NULL OR t.tourId = :tourId)
          AND (CAST(:targetDate AS date) IS NULL OR mg.targetDate = :targetDate)
          AND (CAST(:targetDateFrom AS date) IS NULL OR mg.targetDate >= :targetDateFrom)
          AND (CAST(:targetDateTo AS date) IS NULL OR mg.targetDate <= :targetDateTo)
          AND (
              CAST(:difficulty AS string) IS NULL
              OR (t IS NOT NULL AND UPPER(CAST(t.difficulty AS string)) = UPPER(CAST(:difficulty AS string)))
              OR (cj IS NOT NULL AND UPPER(CAST(cj.difficulty AS string)) = UPPER(CAST(:difficulty AS string)))
          )
          AND (
              CAST(:location AS string) IS NULL
              OR (t IS NOT NULL AND LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%')))
              OR EXISTS (
                  SELECT 1 FROM CustomJourneyCheckpoint cjc
                  WHERE cjc.customJourney = cj
                    AND cjc.isDeleted = false
                    AND LOWER(cjc.locationName) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%'))
              )
          )
          AND (
              CAST(:keyword AS string) = ''
              OR LOWER(mg.groupName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
              OR (t IS NOT NULL AND LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              OR (cj IS NOT NULL AND LOWER(cj.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          )
        """, countQuery = """
        SELECT COUNT(DISTINCT mg) FROM MatchingGroup mg
        LEFT JOIN mg.tour t
        LEFT JOIN mg.customJourney cj
        WHERE mg.isDeleted = false
          AND mg.status = :status
          AND (:availableSlotsOnly = false OR mg.currentSize < mg.maxSize)
          AND mg.matchingDeadline > :now
          AND mg.targetDate > :today
          AND (
              (t IS NOT NULL AND t.isDeleted = false AND t.status = :tourStatus)
              OR (cj IS NOT NULL AND cj.isDeleted = false)
          )
          AND (CAST(:tourId AS uuid) IS NULL OR (t IS NOT NULL AND t.tourId = :tourId))
          AND (
              CAST(:sourceType AS string) IS NULL
              OR (:sourceType = 'TOUR' AND t IS NOT NULL)
              OR (:sourceType = 'CUSTOM_JOURNEY' AND cj IS NOT NULL)
          )
          AND t.isDeleted = false
          AND t.status = :tourStatus
          AND t.vendor.status = com.sep.treksphere.vendor.VendorStatus.ACTIVE
          AND t.vendor.isDeleted = false
          AND (CAST(:tourId AS uuid) IS NULL OR t.tourId = :tourId)
          AND (CAST(:targetDate AS date) IS NULL OR mg.targetDate = :targetDate)
          AND (CAST(:targetDateFrom AS date) IS NULL OR mg.targetDate >= :targetDateFrom)
          AND (CAST(:targetDateTo AS date) IS NULL OR mg.targetDate <= :targetDateTo)
          AND (
              CAST(:difficulty AS string) IS NULL
              OR (t IS NOT NULL AND UPPER(CAST(t.difficulty AS string)) = UPPER(CAST(:difficulty AS string)))
              OR (cj IS NOT NULL AND UPPER(CAST(cj.difficulty AS string)) = UPPER(CAST(:difficulty AS string)))
          )
          AND (
              CAST(:location AS string) IS NULL
              OR (t IS NOT NULL AND LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%')))
              OR EXISTS (
                  SELECT 1 FROM CustomJourneyCheckpoint cjc
                  WHERE cjc.customJourney = cj
                    AND cjc.isDeleted = false
                    AND LOWER(cjc.locationName) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%'))
              )
          )
          AND (
              CAST(:keyword AS string) = ''
              OR LOWER(mg.groupName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
              OR (t IS NOT NULL AND LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              OR (cj IS NOT NULL AND LOWER(cj.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          )
        """)
    Page<MatchingGroup> findAvailableMatchingGroups(
            @Param("status") MatchingGroupStatus status,
            @Param("tourStatus") TourStatus tourStatus,
            @Param("sourceType") String sourceType,
            @Param("tourId") UUID tourId,
            @Param("targetDate") LocalDate targetDate,
            @Param("targetDateFrom") LocalDate targetDateFrom,
            @Param("targetDateTo") LocalDate targetDateTo,
            @Param("difficulty") String difficulty,
            @Param("location") String location,
            @Param("availableSlotsOnly") Boolean availableSlotsOnly,
            @Param("keyword") String keyword,
            @Param("today") LocalDate today,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT mg FROM MatchingGroup mg
        LEFT JOIN FETCH mg.tour t
        LEFT JOIN FETCH mg.customJourney cj
        JOIN FETCH mg.owner o
        LEFT JOIN FETCH mg.members m
        LEFT JOIN FETCH m.user mu
        WHERE mg.matchingGroupId = :id
          AND mg.isDeleted = false
          AND mg.status IN :statuses
          AND (
              (t IS NOT NULL AND t.isDeleted = false AND t.status = :tourStatus)
              OR (cj IS NOT NULL AND cj.isDeleted = false)
          )
          AND t.isDeleted = false
          AND t.status = :tourStatus
          AND t.vendor.status = com.sep.treksphere.vendor.VendorStatus.ACTIVE
          AND t.vendor.isDeleted = false
    """)
    Optional<MatchingGroup> findPublicDetailById(
            @Param("id") UUID id,
            @Param("statuses") Collection<MatchingGroupStatus> statuses,
            @Param("tourStatus") TourStatus tourStatus
    );

    @Query("""
        SELECT mg FROM MatchingGroup mg
        LEFT JOIN FETCH mg.tour t
        LEFT JOIN FETCH mg.customJourney cj
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

    Optional<MatchingGroup> findByConversationConversationId(UUID conversationId);

    boolean existsByTour_TourIdAndStatusInAndIsDeletedFalse(UUID tourId, Collection<MatchingGroupStatus> statuses);

    boolean existsByTour_TourIdAndMaxSizeGreaterThanAndStatusInAndIsDeletedFalse(
            UUID tourId, Integer maxSize, Collection<MatchingGroupStatus> statuses);

    boolean existsByTour_TourIdAndMaxSizeLessThanAndStatusInAndIsDeletedFalse(
            UUID tourId, Integer minSize, Collection<MatchingGroupStatus> statuses);
}
