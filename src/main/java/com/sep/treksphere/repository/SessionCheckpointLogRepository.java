package com.sep.treksphere.repository;

import com.sep.treksphere.entity.SessionCheckpointLog;
import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

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
}
