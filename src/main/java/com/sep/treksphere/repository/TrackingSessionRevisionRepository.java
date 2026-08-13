package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TrackingSessionRevision;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TrackingSessionRevisionRepository extends JpaRepository<TrackingSessionRevision, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM TrackingSessionRevision r WHERE r.tourSessionId = :sessionId")
    Optional<TrackingSessionRevision> findByIdForUpdate(@Param("sessionId") UUID sessionId);
}
