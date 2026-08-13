package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TrackingDeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TrackingDeviceSessionRepository extends JpaRepository<TrackingDeviceSession, UUID> {

    @EntityGraph(attributePaths = {"tourSession", "actor"})
    @Query("SELECT d FROM TrackingDeviceSession d WHERE d.trackingDeviceSessionId = :id")
    Optional<TrackingDeviceSession> findDetailedById(@Param("id") UUID id);

    Optional<TrackingDeviceSession> findByTourSession_TourSessionIdAndActor_UserIdAndDeviceId(
            UUID sessionId,
            UUID actorId,
            UUID deviceId
    );
}
