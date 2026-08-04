package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TourSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import jakarta.persistence.LockModeType;

@Repository
public interface TourSessionRepository extends JpaRepository<TourSession, UUID> {
    Optional<TourSession> findByTourSessionIdAndIsDeletedFalse(UUID tourSessionId);
    Optional<TourSession> findByTourSchedule_ScheduleIdAndIsDeletedFalse(UUID scheduleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TourSession ts " +
           "WHERE ts.tourSessionId = :tourSessionId AND ts.isDeleted = false")
    Optional<TourSession> findByIdForUpdate(@Param("tourSessionId") UUID tourSessionId);

    @Query("SELECT ts FROM TourSession ts " +
           "JOIN FETCH ts.tourSchedule sch " +
           "JOIN FETCH sch.tour t " +
           "JOIN FETCH t.vendor v " +
           "WHERE ts.tourSessionId = :tourSessionId AND ts.isDeleted = false")
    Optional<TourSession> findByIdWithVendor(@Param("tourSessionId") UUID tourSessionId);

    @Query(value = "SELECT ts FROM TourSession ts " +
           "JOIN FETCH ts.tourSchedule sch " +
           "JOIN FETCH sch.tour t " +
           "WHERE t.vendor.vendorId = :vendorId " +
           "AND ts.isDeleted = false " +
           "AND (:tourId IS NULL OR t.tourId = :tourId) " +
           "AND (:status IS NULL OR ts.status = :status)",
           countQuery = "SELECT COUNT(ts) FROM TourSession ts " +
           "JOIN ts.tourSchedule sch " +
           "JOIN sch.tour t " +
           "WHERE t.vendor.vendorId = :vendorId " +
           "AND ts.isDeleted = false " +
           "AND (:tourId IS NULL OR t.tourId = :tourId) " +
           "AND (:status IS NULL OR ts.status = :status)")
    Page<TourSession> findByVendorAndFilters(@Param("vendorId") UUID vendorId,
                                             @Param("tourId") UUID tourId,
                                             @Param("status") TourSessionStatus status,
                                             Pageable pageable);
}
