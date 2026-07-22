package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    boolean existsByScheduleAndBookingStatusNotAndIsDeletedFalse(TourSchedule schedule, BookingStatus bookingStatus);

    @Query("SELECT b FROM Booking b WHERE b.user = :user AND (:status IS NULL OR b.bookingStatus = :status) AND b.isDeleted = false")
    Page<Booking> findByUserAndFilters(@Param("user") User user, @Param("status") BookingStatus status, Pageable pageable);
}
