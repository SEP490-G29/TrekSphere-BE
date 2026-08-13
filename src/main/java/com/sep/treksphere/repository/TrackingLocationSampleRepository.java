package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TrackingLocationSample;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingLocationSampleRepository extends JpaRepository<TrackingLocationSample, UUID> {

    Optional<TrackingLocationSample> findFirstByTourSessionIdAndActorIdOrderByRecordedAtDesc(
            UUID sessionId,
            UUID actorId
    );

    List<TrackingLocationSample> findByTourSessionIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID sessionId,
            Instant from,
            Instant to,
            Pageable pageable
    );

    List<TrackingLocationSample> findByTourSessionIdAndActorIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID sessionId,
            UUID actorId,
            Instant from,
            Instant to,
            Pageable pageable
    );

    List<TrackingLocationSample> findByTourSessionIdOrderByRecordedAtDesc(UUID sessionId, Pageable pageable);

    @Query("SELECT s FROM TrackingLocationSample s WHERE s.tourSessionId = :sessionId " +
           "AND s.recordedAt = (SELECT MAX(s2.recordedAt) FROM TrackingLocationSample s2 " +
           "WHERE s2.tourSessionId = :sessionId AND s2.actorId = s.actorId)")
    List<TrackingLocationSample> findLatestBySessionId(@Param("sessionId") UUID sessionId);
}
