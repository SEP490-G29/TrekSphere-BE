package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentStatus;
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

    @Query("SELECT b FROM Booking b WHERE b.schedule.tour.vendor.vendorId = :vendorId " +
           "AND b.isDeleted = false " +
           "AND (:bookingStatus IS NULL OR b.bookingStatus = :bookingStatus) " +
           "AND (:paymentStatus IS NULL OR b.paymentStatus = :paymentStatus) " +
           "AND (:tourId IS NULL OR b.schedule.tour.tourId = :tourId) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(b.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(b.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(b.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Booking> findVendorBookings(@Param("vendorId") UUID vendorId,
                                    @Param("bookingStatus") BookingStatus bookingStatus,
                                    @Param("paymentStatus") PaymentStatus paymentStatus,
                                    @Param("tourId") UUID tourId,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);
}
