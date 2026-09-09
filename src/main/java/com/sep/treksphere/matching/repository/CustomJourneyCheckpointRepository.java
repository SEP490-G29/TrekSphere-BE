package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomJourneyCheckpointRepository extends JpaRepository<CustomJourneyCheckpoint, UUID> {

    List<CustomJourneyCheckpoint> findByCustomJourney_CustomJourneyIdAndIsDeletedFalseOrderByDayNoAscCheckpointOrderAsc(UUID customJourneyId);

    List<CustomJourneyCheckpoint> findByCustomJourney_CustomJourneyIdAndIsDeletedFalseOrderByCheckpointOrderAsc(UUID customJourneyId);

    Optional<CustomJourneyCheckpoint> findByCustomJourneyCheckpointIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(
            UUID checkpointId, UUID customJourneyId);

    boolean existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndIsDeletedFalse(
            UUID customJourneyId, Integer checkpointOrder);

    boolean existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndCustomJourneyCheckpointIdNotAndIsDeletedFalse(
            UUID customJourneyId, Integer checkpointOrder, UUID checkpointId);
}
