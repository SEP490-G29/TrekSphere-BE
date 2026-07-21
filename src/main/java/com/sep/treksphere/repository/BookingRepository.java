package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.enums.booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    boolean existsByScheduleAndBookingStatusNotAndIsDeletedFalse(TourSchedule schedule, BookingStatus bookingStatus);
}
