package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TrackingIngestedEvent;
import com.sep.treksphere.enums.tracking.TrackingEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingIngestedEventRepository extends JpaRepository<TrackingIngestedEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM TrackingIngestedEvent e WHERE e.trackingIngestedEventId = :eventId")
    Optional<TrackingIngestedEvent> findByIdForUpdate(@Param("eventId") UUID eventId);

    Optional<TrackingIngestedEvent> findByActorIdAndDeviceIdAndClientEventId(
            UUID actorId,
            UUID deviceId,
            UUID clientEventId
    );

    Optional<TrackingIngestedEvent> findByTourSessionIdAndActorIdAndDeviceIdAndSequenceNumber(
            UUID sessionId,
            UUID actorId,
            UUID deviceId,
            Long sequenceNumber
    );

    List<TrackingIngestedEvent> findTop100ByTourSessionIdAndProcessingStatusAndResultRevisionGreaterThanOrderByResultRevisionAsc(
            UUID sessionId,
            TrackingEventStatus status,
            Long revision
    );
}
