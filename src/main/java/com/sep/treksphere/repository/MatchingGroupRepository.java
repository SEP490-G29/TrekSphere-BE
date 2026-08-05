package com.sep.treksphere.repository;

import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
        WHERE mg.isDeleted = false
          AND mg.status = :status
          AND (:tourId IS NULL OR t.tourId = :tourId)
          AND (:targetDate IS NULL OR mg.targetDate = :targetDate)
        """, countQuery = """
        SELECT COUNT(mg) FROM MatchingGroup mg
        WHERE mg.isDeleted = false
          AND mg.status = :status
          AND (:tourId IS NULL OR mg.tour.tourId = :tourId)
          AND (:targetDate IS NULL OR mg.targetDate = :targetDate)
        """)
    Page<MatchingGroup> findAvailableMatchingGroups(
            @Param("status") MatchingGroupStatus status,
            @Param("tourId") UUID tourId,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
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

