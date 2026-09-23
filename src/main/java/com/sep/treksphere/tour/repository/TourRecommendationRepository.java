package com.sep.treksphere.tour.repository;

import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.enums.DifficultyLevel;
import com.sep.treksphere.tour.enums.TourBehaviorEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TourRecommendationRepository extends Repository<Tour, UUID> {

    interface BehaviorSignalProjection {
        UUID getTourId();

        String getLocation();

        DifficultyLevel getDifficulty();
    }

    @Query("""
            SELECT DISTINCT t.location
            FROM MatchingGroup mg
            JOIN mg.tour t
            WHERE mg.isDeleted = false
              AND mg.status = com.sep.treksphere.matching.enums.MatchingGroupStatus.COMPLETED
              AND t.isDeleted = false
              AND (
                  mg.owner.userId = :userId
                  OR EXISTS (
                      SELECT 1
                      FROM MatchingMember mm
                      WHERE mm.matchingGroup = mg
                        AND mm.user.userId = :userId
                        AND mm.status = com.sep.treksphere.matching.enums.JoinStatus.ACCEPTED
                        AND mm.isDeleted = false
                  )
              )
            """)
    List<String> findCompletedTourLocations(@Param("userId") UUID userId);

    @Query("""
            SELECT t.difficulty
            FROM MatchingGroup mg
            JOIN mg.tour t
            WHERE mg.isDeleted = false
              AND mg.status = com.sep.treksphere.matching.enums.MatchingGroupStatus.COMPLETED
              AND t.isDeleted = false
              AND (
                  mg.owner.userId = :userId
                  OR EXISTS (
                      SELECT 1
                      FROM MatchingMember mm
                      WHERE mm.matchingGroup = mg
                        AND mm.user.userId = :userId
                        AND mm.status = com.sep.treksphere.matching.enums.JoinStatus.ACCEPTED
                        AND mm.isDeleted = false
                  )
              )
            """)
    List<DifficultyLevel> findCompletedTourDifficulties(@Param("userId") UUID userId);

    @Query("""
            SELECT mg.tour.tourId
            FROM MatchingGroup mg
            WHERE mg.isDeleted = false
              AND mg.tour.tourId IN :tourIds
              AND mg.status IN :statuses
            GROUP BY mg.tour.tourId
            HAVING COUNT(mg) >= :minimumGroupCount
            """)
    List<UUID> findPopularTourIds(
            @Param("tourIds") Collection<UUID> tourIds,
            @Param("statuses") Collection<MatchingGroupStatus> statuses,
            @Param("minimumGroupCount") long minimumGroupCount);

    @Query("""
            SELECT DISTINCT event.tour.tourId AS tourId,
                            event.tour.location AS location,
                            event.tour.difficulty AS difficulty
            FROM TourBehaviorEvent event
            WHERE event.user.userId = :userId
              AND event.eventType IN :positiveTypes
              AND event.occurredAt >= :since
              AND NOT EXISTS (
                  SELECT newer.behaviorEventId
                  FROM TourBehaviorEvent newer
                  WHERE newer.user = event.user
                    AND newer.tour = event.tour
                    AND newer.eventType IN :negativeTypes
                    AND newer.occurredAt > event.occurredAt
              )
            """)
    List<BehaviorSignalProjection> findRecentPositiveBehaviorSignals(
            @Param("userId") UUID userId,
            @Param("since") LocalDateTime since,
            @Param("positiveTypes") Collection<TourBehaviorEventType> positiveTypes,
            @Param("negativeTypes") Collection<TourBehaviorEventType> negativeTypes);

    @Query("""
            SELECT schedule.tour.tourId
            FROM TourSchedule schedule
            WHERE schedule.tour.tourId IN :tourIds
              AND schedule.isDeleted = false
              AND schedule.status = com.sep.treksphere.tour.enums.ScheduleStatus.OPEN
              AND schedule.departureDate > :today
            GROUP BY schedule.tour.tourId
            HAVING COUNT(schedule) >= :minimumScheduleCount
            """)
    List<UUID> findTourIdsWithFutureOpenSchedules(
            @Param("tourIds") Collection<UUID> tourIds,
            @Param("today") LocalDate today,
            @Param("minimumScheduleCount") long minimumScheduleCount);

    @Query("""
            SELECT matchingGroup.tour.tourId
            FROM MatchingGroup matchingGroup
            WHERE matchingGroup.tour.tourId IN :tourIds
              AND matchingGroup.isDeleted = false
              AND matchingGroup.status = com.sep.treksphere.matching.enums.MatchingGroupStatus.OPEN
              AND matchingGroup.targetDate > :today
              AND matchingGroup.currentSize < matchingGroup.maxSize
            GROUP BY matchingGroup.tour.tourId
            """)
    List<UUID> findTourIdsWithAvailableGroups(
            @Param("tourIds") Collection<UUID> tourIds,
            @Param("today") LocalDate today);

    @Query(value = """
            SELECT t.*
            FROM tour t
            JOIN vendor v ON v.vendor_id = t.vendor_id
            WHERE t.is_deleted = FALSE
              AND t.status = 'PUBLISHED'
              AND v.is_deleted = FALSE
              AND v.status = 'ACTIVE'
              AND EXISTS (
                  SELECT 1
                  FROM tour_schedule ts
                  WHERE ts.tour_id = t.tour_id
                    AND ts.is_deleted = FALSE
                    AND ts.status = 'OPEN'
                    AND ts.departure_date > CURRENT_DATE
              )
              AND (
                  CAST(:maxDifficultyRank AS integer) IS NULL
                  OR CASE t.difficulty
                       WHEN 'EASY' THEN 0
                       WHEN 'MODERATE' THEN 1
                       WHEN 'HARD' THEN 2
                       WHEN 'EXTREME' THEN 3
                       ELSE 99
                     END <= :maxDifficultyRank
              )
            ORDER BY (
                CASE WHEN EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(
                        CAST(:preferredAreasJson AS jsonb)
                    ) AS preferred_area(value)
                    WHERE POSITION(
                              LOWER(BTRIM(preferred_area.value))
                              IN LOWER(BTRIM(t.location))
                          ) > 0
                       OR POSITION(
                              LOWER(BTRIM(t.location))
                              IN LOWER(BTRIM(preferred_area.value))
                          ) > 0
                ) THEN 60 ELSE 0 END
                + CASE WHEN EXISTS (
                    SELECT 1
                    FROM jsonb_array_elements_text(
                        CAST(:historyLocationsJson AS jsonb)
                    ) AS history_location(value)
                    WHERE POSITION(
                              LOWER(BTRIM(history_location.value))
                              IN LOWER(BTRIM(t.location))
                          ) > 0
                       OR POSITION(
                              LOWER(BTRIM(t.location))
                              IN LOWER(BTRIM(history_location.value))
                          ) > 0
                ) THEN 25 ELSE 0 END
                + CASE WHEN CAST(:preferredDifficulty AS varchar) IS NOT NULL
                            AND t.difficulty = :preferredDifficulty
                       THEN 30 ELSE 0 END
                + CASE WHEN CAST(:progressionDifficulty AS varchar) IS NOT NULL
                            AND t.difficulty = :progressionDifficulty
                       THEN 20 ELSE 0 END
                + CASE
                    WHEN CAST(:maxDifficultyRank AS integer) IS NULL THEN 0
                    WHEN CASE t.difficulty
                           WHEN 'EASY' THEN 0 WHEN 'MODERATE' THEN 1
                           WHEN 'HARD' THEN 2 WHEN 'EXTREME' THEN 3 ELSE 99
                         END = :maxDifficultyRank THEN 15
                    WHEN CASE t.difficulty
                           WHEN 'EASY' THEN 0 WHEN 'MODERATE' THEN 1
                           WHEN 'HARD' THEN 2 WHEN 'EXTREME' THEN 3 ELSE 99
                         END = :maxDifficultyRank - 1 THEN 8
                    ELSE 3
                  END
                + GREATEST(-80, LEAST(80, COALESCE((
                    SELECT SUM(
                        CASE behavior.event_type
                          WHEN 'IMPRESSION' THEN -1
                          WHEN 'VIEW' THEN 4
                          WHEN 'CLICK' THEN 8
                          WHEN 'SAVE' THEN 25
                          WHEN 'UNSAVE' THEN -25
                          WHEN 'DISMISS' THEN -45
                          ELSE 0
                        END
                        * CASE
                            WHEN behavior.occurred_at >= CURRENT_TIMESTAMP - INTERVAL '7 days'
                              THEN 1.0
                            WHEN behavior.occurred_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
                              THEN 0.7
                            WHEN behavior.occurred_at >= CURRENT_TIMESTAMP - INTERVAL '90 days'
                              THEN 0.4
                            ELSE 0.15
                          END
                        * CASE
                            WHEN behavior.tour_id = t.tour_id THEN 1.0
                            WHEN behavior.event_type IN ('IMPRESSION', 'UNSAVE', 'DISMISS')
                              THEN 0
                            WHEN POSITION(
                                     LOWER(BTRIM(behavior_tour.location))
                                     IN LOWER(BTRIM(t.location))
                                 ) > 0
                              OR POSITION(
                                     LOWER(BTRIM(t.location))
                                     IN LOWER(BTRIM(behavior_tour.location))
                                 ) > 0
                              THEN 0.6
                            WHEN behavior_tour.difficulty = t.difficulty THEN 0.3
                            ELSE 0
                          END
                    )
                    FROM tour_behavior_event behavior
                    JOIN tour behavior_tour ON behavior_tour.tour_id = behavior.tour_id
                    WHERE behavior.user_id = :userId
                      AND behavior.occurred_at >= CURRENT_TIMESTAMP - INTERVAL '180 days'
                ), 0)))
                + LEAST((
                    SELECT COUNT(*)
                    FROM tour_schedule flexible_schedule
                    WHERE flexible_schedule.tour_id = t.tour_id
                      AND flexible_schedule.is_deleted = FALSE
                      AND flexible_schedule.status = 'OPEN'
                      AND flexible_schedule.departure_date > CURRENT_DATE
                  ), 3) * 2
                + LEAST(COALESCE((
                    SELECT SUM(GREATEST(
                        available_group.max_size - available_group.current_size, 0
                    ))
                    FROM matching_group available_group
                    WHERE available_group.tour_id = t.tour_id
                      AND available_group.is_deleted = FALSE
                      AND available_group.status = 'OPEN'
                      AND available_group.target_date > CURRENT_DATE
                  ), 0), 10)
                + LEAST((
                    SELECT COUNT(*)
                    FROM matching_group popularity_group
                    WHERE popularity_group.tour_id = t.tour_id
                      AND popularity_group.is_deleted = FALSE
                      AND popularity_group.status IN (
                          'OPEN', 'FULL', 'CLOSED', 'IN_PROGRESS', 'COMPLETED'
                      )
                  ), 5) * 2
                - CASE WHEN EXISTS (
                    SELECT 1
                    FROM matching_group completed_group
                    WHERE completed_group.tour_id = t.tour_id
                      AND completed_group.is_deleted = FALSE
                      AND completed_group.status = 'COMPLETED'
                      AND (
                          completed_group.owner_id = :userId
                          OR EXISTS (
                              SELECT 1
                              FROM matching_member completed_member
                              WHERE completed_member.matching_group_id =
                                    completed_group.matching_group_id
                                AND completed_member.user_id = :userId
                                AND completed_member.status = 'ACCEPTED'
                                AND completed_member.is_deleted = FALSE
                          )
                      )
                ) THEN 200 ELSE 0 END
            ) DESC,
            t.published_at DESC NULLS LAST,
            t.tour_id
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM tour t
            JOIN vendor v ON v.vendor_id = t.vendor_id
            WHERE t.is_deleted = FALSE
              AND t.status = 'PUBLISHED'
              AND v.is_deleted = FALSE
              AND v.status = 'ACTIVE'
              AND EXISTS (
                  SELECT 1
                  FROM tour_schedule ts
                  WHERE ts.tour_id = t.tour_id
                    AND ts.is_deleted = FALSE
                    AND ts.status = 'OPEN'
                    AND ts.departure_date > CURRENT_DATE
              )
              AND (
                  CAST(:maxDifficultyRank AS integer) IS NULL
                  OR CASE t.difficulty
                       WHEN 'EASY' THEN 0
                       WHEN 'MODERATE' THEN 1
                       WHEN 'HARD' THEN 2
                       WHEN 'EXTREME' THEN 3
                       ELSE 99
                     END <= :maxDifficultyRank
              )
            """,
            nativeQuery = true)
    Page<Tour> findPersonalizedRecommendations(
            @Param("userId") UUID userId,
            @Param("preferredAreasJson") String preferredAreasJson,
            @Param("historyLocationsJson") String historyLocationsJson,
            @Param("preferredDifficulty") String preferredDifficulty,
            @Param("progressionDifficulty") String progressionDifficulty,
            @Param("maxDifficultyRank") Integer maxDifficultyRank,
            Pageable pageable);
}
