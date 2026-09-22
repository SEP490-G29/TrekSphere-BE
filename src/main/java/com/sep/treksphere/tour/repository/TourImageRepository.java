package com.sep.treksphere.tour.repository;

import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.entity.TourImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TourImageRepository extends JpaRepository<TourImage, UUID> {

    List<TourImage> findByTourOrderBySortOrderAsc(Tour tour);

    List<TourImage> findByTourAndIsDeletedFalseOrderBySortOrderAsc(Tour tour);

    @Modifying
    @Query("UPDATE TourImage ti SET ti.isDeleted = true, ti.deletedAt = :deletedAt, ti.deletedBy = :deletedBy " +
           "WHERE ti.tour.tourId = :tourId AND ti.isDeleted = false")
    int softDeleteByTourId(@Param("tourId") UUID tourId,
                           @Param("deletedAt") LocalDateTime deletedAt,
                           @Param("deletedBy") String deletedBy);

    @Modifying
    @Query("UPDATE TourImage ti SET ti.isDeleted = false, ti.deletedAt = null, ti.deletedBy = null " +
           "WHERE ti.tour.tourId = :tourId AND ti.deletedAt = :deletedAt AND ti.isDeleted = true")
    int restoreByTourIdAndDeletedAt(@Param("tourId") UUID tourId,
                                    @Param("deletedAt") LocalDateTime deletedAt);
}
