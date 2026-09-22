package com.sep.treksphere.tour.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TourBehaviorEventRepository extends JpaRepository<TourBehaviorEvent, UUID> {

    @Query("""
            SELECT event
            FROM TourBehaviorEvent event
            WHERE event.user.userId = :userId
              AND event.tour.tourId IN :tourIds
              AND event.occurredAt >= :since
            """)
    List<TourBehaviorEvent> findRecentEvents(
            @Param("userId") UUID userId,
            @Param("tourIds") Collection<UUID> tourIds,
            @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM TourBehaviorEvent event WHERE event.user.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM TourBehaviorEvent event WHERE event.occurredAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
