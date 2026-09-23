package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomJourneyCheckpointRepository extends JpaRepository<CustomJourneyCheckpoint, UUID> {

    List<CustomJourneyCheckpoint> findByCustomJourney_CustomJourneyIdAndIsDeletedFalseOrderByDayNoAscCheckpointOrderAsc(UUID customJourneyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomJourneyCheckpoint c where c.customJourneyCheckpointId = :checkpointId and c.isDeleted = false")
    Optional<CustomJourneyCheckpoint> findByIdForUpdate(@Param("checkpointId") UUID checkpointId);

    List<CustomJourneyCheckpoint> findByCustomJourney_CustomJourneyIdAndIsDeletedFalseOrderByCheckpointOrderAsc(UUID customJourneyId);

    Optional<CustomJourneyCheckpoint> findByCustomJourneyCheckpointIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(
            UUID checkpointId, UUID customJourneyId);

    boolean existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndIsDeletedFalse(
            UUID customJourneyId, Integer checkpointOrder);

    boolean existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndCustomJourneyCheckpointIdNotAndIsDeletedFalse(
            UUID customJourneyId, Integer checkpointOrder, UUID checkpointId);

    long countByCustomJourney_CustomJourneyIdAndIsDeletedFalse(UUID customJourneyId);

    List<CustomJourneyCheckpoint> findByCustomJourney_CustomJourneyIdAndCheckpointOrderGreaterThanAndIsDeletedFalseOrderByCheckpointOrderAsc(
            UUID customJourneyId, Integer checkpointOrder);
}
