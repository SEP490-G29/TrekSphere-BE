package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourScheduleRepository extends JpaRepository<TourSchedule, UUID> {
    List<TourSchedule> findByTourAndStatusOrderByDepartureDateAsc(Tour tour, ScheduleStatus status);
    List<TourSchedule> findByTourAndIsDeletedFalseOrderByDepartureDateAsc(Tour tour);
    List<TourSchedule> findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(Tour tour, ScheduleStatus status, LocalDate date);
    Optional<TourSchedule> findByScheduleIdAndIsDeletedFalse(UUID scheduleId);

    /**
     * Cascade soft delete: đánh dấu xóa mềm tất cả schedule chưa bị xóa của tour,
     * gán chung deletedAt timestamp để phục vụ restore đúng đợt.
     */
    @Modifying
    @Query("UPDATE TourSchedule ts SET ts.isDeleted = true, ts.deletedAt = :deletedAt, ts.deletedBy = :deletedBy " +
           "WHERE ts.tour.tourId = :tourId AND ts.isDeleted = false")
    int softDeleteByTourId(@Param("tourId") UUID tourId,
                           @Param("deletedAt") LocalDateTime deletedAt,
                           @Param("deletedBy") String deletedBy);

    /**
     * Restore: khôi phục schedule bị xóa cùng đợt với tour (match exact deletedAt).
     */
    @Modifying
    @Query("UPDATE TourSchedule ts SET ts.isDeleted = false, ts.deletedAt = null, ts.deletedBy = null " +
           "WHERE ts.tour.tourId = :tourId AND ts.deletedAt = :deletedAt AND ts.isDeleted = true")
    int restoreByTourIdAndDeletedAt(@Param("tourId") UUID tourId,
                                    @Param("deletedAt") LocalDateTime deletedAt);
}
