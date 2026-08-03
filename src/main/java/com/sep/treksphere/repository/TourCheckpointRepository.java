package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourCheckpoint;
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

    boolean existsByTourAndCheckpointOrderAndIsDeletedFalse(Tour tour, Integer checkpointOrder);

    boolean existsByTourAndCheckpointOrderAndCheckpointIdNotAndIsDeletedFalse(
            Tour tour, Integer checkpointOrder, UUID checkpointId);

    boolean existsByTourAndCheckpointNameIgnoreCaseAndIsDeletedFalse(Tour tour, String checkpointName);

    boolean existsByTourAndCheckpointNameIgnoreCaseAndCheckpointIdNotAndIsDeletedFalse(
            Tour tour, String checkpointName, UUID checkpointId);

    boolean existsByTourAndLatitudeAndLongitudeAndIsDeletedFalse(
            Tour tour, BigDecimal latitude, BigDecimal longitude);

    boolean existsByTourAndLatitudeAndLongitudeAndCheckpointIdNotAndIsDeletedFalse(
            Tour tour, BigDecimal latitude, BigDecimal longitude, UUID checkpointId);

    /**
     * Cascade soft delete: đánh dấu xóa mềm tất cả checkpoint chưa bị xóa của tour,
     * gán chung deletedAt timestamp để phục vụ restore đúng đợt.
     */
    @Modifying
    @Query("UPDATE TourCheckpoint tc SET tc.isDeleted = true, tc.deletedAt = :deletedAt, tc.deletedBy = :deletedBy " +
           "WHERE tc.tour.tourId = :tourId AND tc.isDeleted = false")
    int softDeleteByTourId(@Param("tourId") UUID tourId,
                           @Param("deletedAt") LocalDateTime deletedAt,
                           @Param("deletedBy") String deletedBy);

    /**
     * Restore: khôi phục checkpoint bị xóa cùng đợt với tour (match exact deletedAt).
     */
    @Modifying
    @Query("UPDATE TourCheckpoint tc SET tc.isDeleted = false, tc.deletedAt = null, tc.deletedBy = null " +
           "WHERE tc.tour.tourId = :tourId AND tc.deletedAt = :deletedAt AND tc.isDeleted = true")
    int restoreByTourIdAndDeletedAt(@Param("tourId") UUID tourId,
                                    @Param("deletedAt") LocalDateTime deletedAt);
}
