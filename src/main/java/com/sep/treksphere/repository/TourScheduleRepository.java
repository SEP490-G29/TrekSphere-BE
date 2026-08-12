package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

@Repository
public interface TourScheduleRepository extends JpaRepository<TourSchedule, UUID> {
       List<TourSchedule> findByTourAndStatusOrderByDepartureDateAsc(Tour tour, ScheduleStatus status);

       List<TourSchedule> findByTourAndIsDeletedFalseOrderByDepartureDateAsc(Tour tour);

       List<TourSchedule> findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(
                     Tour tour, ScheduleStatus status, LocalDate date);

       Optional<TourSchedule> findByScheduleIdAndIsDeletedFalse(UUID scheduleId);

       @Query("SELECT ts FROM TourSchedule ts WHERE ts.tour.vendor.vendorId = :vendorId " +
                     "AND ts.isDeleted = false AND ts.departureDate >= :today ORDER BY ts.departureDate ASC")
       List<TourSchedule> findVendorUpcomingSchedules(
                     @Param("vendorId") UUID vendorId, @Param("today") LocalDate today);

       @Query("SELECT ts FROM TourSchedule ts WHERE ts.tour.vendor.vendorId = :vendorId " +
                     "AND ts.isDeleted = false AND ts.createdAt >= :startDate AND ts.createdAt <= :endDate")
       List<TourSchedule> findVendorSchedulesInPeriod(
                     @Param("vendorId") UUID vendorId,
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Modifying
       @Query("UPDATE TourSchedule ts SET ts.isDeleted = true, ts.deletedAt = :deletedAt, " +
                     "ts.deletedBy = :deletedBy WHERE ts.tour.tourId = :tourId AND ts.isDeleted = false")
       int softDeleteByTourId(
                     @Param("tourId") UUID tourId,
                     @Param("deletedAt") LocalDateTime deletedAt,
                     @Param("deletedBy") String deletedBy);

       @Modifying
       @Query("UPDATE TourSchedule ts SET ts.isDeleted = false, ts.deletedAt = null, ts.deletedBy = null " +
                     "WHERE ts.tour.tourId = :tourId AND ts.deletedAt = :deletedAt AND ts.isDeleted = true")
       int restoreByTourIdAndDeletedAt(
                     @Param("tourId") UUID tourId, @Param("deletedAt") LocalDateTime deletedAt);

       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("select s from TourSchedule s join fetch s.tour t join fetch t.vendor " +
                     "where s.scheduleId = :scheduleId and s.isDeleted = false")
       Optional<TourSchedule> findByIdForUpdate(@Param("scheduleId") UUID scheduleId);
}
