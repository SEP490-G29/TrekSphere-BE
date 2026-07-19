package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TourCheckpointRepository extends JpaRepository<TourCheckpoint, UUID> {

    List<TourCheckpoint> findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(Tour tour);

    boolean existsByTourAndCheckpointOrderAndIsDeletedFalse(Tour tour, Integer checkpointOrder);

    boolean existsByTourAndCheckpointOrderAndCheckpointIdNotAndIsDeletedFalse(
            Tour tour, Integer checkpointOrder, UUID checkpointId);
}
