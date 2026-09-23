package com.sep.treksphere.tour.repository;

import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.entity.TourCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TourCheckpointRepository extends JpaRepository<TourCheckpoint, UUID> {

    List<TourCheckpoint> findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(Tour tour);

    long countByTourAndIsDeletedFalse(Tour tour);

    boolean existsByTourAndCheckpointOrderAndIsDeletedFalse(Tour tour, Integer checkpointOrder);

    boolean existsByTourAndCheckpointOrderAndTourCheckpointIdNotAndIsDeletedFalse(
            Tour tour, Integer checkpointOrder, UUID tourCheckpointId);

    boolean existsByTourAndCheckpointNameIgnoreCaseAndIsDeletedFalse(Tour tour, String checkpointName);

    boolean existsByTourAndCheckpointNameIgnoreCaseAndTourCheckpointIdNotAndIsDeletedFalse(
            Tour tour, String checkpointName, UUID tourCheckpointId);

    boolean existsByTourAndLatitudeAndLongitudeAndIsDeletedFalse(
            Tour tour, BigDecimal latitude, BigDecimal longitude);

    boolean existsByTourAndLatitudeAndLongitudeAndTourCheckpointIdNotAndIsDeletedFalse(
            Tour tour, BigDecimal latitude, BigDecimal longitude, UUID tourCheckpointId);

    @Modifying
    @Query("UPDATE TourCheckpoint tc SET tc.isDeleted = true, tc.deletedAt = :deletedAt, tc.deletedBy = :deletedBy " +
           "WHERE tc.tour.tourId = :tourId AND tc.isDeleted = false")
    int softDeleteByTourId(@Param("tourId") UUID tourId,
                           @Param("deletedAt") LocalDateTime deletedAt,
                           @Param("deletedBy") String deletedBy);

    @Modifying
    @Query("UPDATE TourCheckpoint tc SET tc.isDeleted = false, tc.deletedAt = null, tc.deletedBy = null " +
           "WHERE tc.tour.tourId = :tourId AND tc.deletedAt = :deletedAt AND tc.isDeleted = true")
    int restoreByTourIdAndDeletedAt(@Param("tourId") UUID tourId,
                                    @Param("deletedAt") LocalDateTime deletedAt);
}
