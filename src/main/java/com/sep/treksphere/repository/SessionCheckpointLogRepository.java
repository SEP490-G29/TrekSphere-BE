package com.sep.treksphere.repository;

import com.sep.treksphere.entity.SessionCheckpointLog;
import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface SessionCheckpointLogRepository extends JpaRepository<SessionCheckpointLog, UUID> {

    boolean existsByTourSession_TourSessionIdAndIsDeletedFalse(UUID tourSessionId);

    List<SessionCheckpointLog> findByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCheckpoint_CheckpointOrderAsc(
            UUID tourSessionId,
            SessionCheckpointLogStatus status
    );

    List<SessionCheckpointLog> findByTourSession_TourSessionIdAndIsDeletedFalseOrderByCheckpoint_CheckpointOrderAsc(
            UUID tourSessionId
    );

    Optional<SessionCheckpointLog> findByTourSession_TourSessionIdAndCheckpoint_CheckpointIdAndIsDeletedFalse(
            UUID tourSessionId,
            UUID checkpointId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM SessionCheckpointLog l WHERE l.tourSession.tourSessionId = :sessionId " +
           "AND l.checkpoint.checkpointId = :checkpointId AND l.isDeleted = false")
    Optional<SessionCheckpointLog> findBySessionAndCheckpointForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("checkpointId") UUID checkpointId
    );
}
