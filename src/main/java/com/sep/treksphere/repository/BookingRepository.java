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
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
        @Query("select b.schedule.scheduleId from Booking b where b.bookingId = :bookingId and b.isDeleted = false")
        Optional<UUID> findScheduleIdByBookingId(@Param("bookingId") UUID bookingId);

        boolean existsByScheduleAndBookingStatusNotAndIsDeletedFalse(TourSchedule schedule,
                        BookingStatus bookingStatus);

        List<Booking> findByScheduleAndBookingStatusNotAndIsDeletedFalse(
                        TourSchedule schedule, BookingStatus bookingStatus);

        boolean existsByScheduleAndBookingStatusInAndIsDeletedFalse(
                        TourSchedule schedule, java.util.Collection<BookingStatus> statuses);

        @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
                        "WHERE b.schedule.tour.tourId = :tourId " +
                        "AND b.bookingStatus NOT IN (" +
                        "com.sep.treksphere.enums.booking.BookingStatus.CANCELLED, " +
                        "com.sep.treksphere.enums.booking.BookingStatus.EXPIRED, " +
                        "com.sep.treksphere.enums.booking.BookingStatus.REJECTED) " +
                        "AND b.isDeleted = false")
        boolean existsActiveBookingByTourId(@Param("tourId") UUID tourId);

        boolean existsByUser_UserIdAndSchedule_ScheduleIdAndBookingStatusAndIsDeletedFalse(
                        UUID userId, UUID scheduleId, BookingStatus bookingStatus);

        @Query("SELECT b FROM Booking b WHERE b.user = :user AND (CAST(:status AS string) IS NULL OR b.bookingStatus = :status) AND b.isDeleted = false")
        Page<Booking> findByUserAndFilters(@Param("user") User user, @Param("status") BookingStatus status,
                        Pageable pageable);

        @Query("SELECT b FROM Booking b WHERE b.schedule.tour.vendor.vendorId = :vendorId " +
                        "AND b.isDeleted = false " +
                        "AND b.createdAt >= :startDate AND b.createdAt <= :endDate")
        java.util.List<Booking> findVendorBookingsInPeriod(@Param("vendorId") UUID vendorId,
                        @Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate);

        @Query("SELECT b FROM Booking b WHERE b.schedule.scheduleId = :scheduleId AND b.isDeleted = false")
        java.util.List<Booking> findByScheduleId(@Param("scheduleId") UUID scheduleId);

        @Query("SELECT b FROM Booking b WHERE b.schedule.tour.vendor.vendorId = :vendorId " +
                        "AND b.isDeleted = false " +
                        "AND (CAST(:bookingStatus AS string) IS NULL OR b.bookingStatus = :bookingStatus) " +
                        "AND (CAST(:paymentStatus AS string) IS NULL OR b.paymentStatus = :paymentStatus) " +
                        "AND (CAST(:tourId AS uuid) IS NULL OR b.schedule.tour.tourId = :tourId) " +
                        "AND (CAST(:keyword AS string) IS NULL OR CAST(:keyword AS string) = '' OR " +
                        "     LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
                        "     LOWER(b.user.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
                        "     LOWER(b.user.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
                        "     LOWER(b.user.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
        Page<Booking> findVendorBookings(@Param("vendorId") UUID vendorId,
                        @Param("bookingStatus") BookingStatus bookingStatus,
                        @Param("paymentStatus") PaymentStatus paymentStatus,
                        @Param("tourId") UUID tourId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select b from Booking b join fetch b.schedule s join fetch s.tour t join fetch t.vendor " +
                        "where b.bookingId = :bookingId and b.isDeleted = false")
        Optional<Booking> findByIdForUpdate(@Param("bookingId") UUID bookingId);

        List<Booking> findTop100ByBookingStatusAndHoldExpiresAtBeforeAndIsDeletedFalseOrderByHoldExpiresAtAsc(
                        BookingStatus status, LocalDateTime now);

        List<Booking> findTop100ByBookingStatusAndConfirmationExpiresAtBeforeAndIsDeletedFalseOrderByConfirmationExpiresAtAsc(
                        BookingStatus status, LocalDateTime now);

        List<Booking> findTop100ByPaymentPlanAndPaymentStatusAndRemainingDueAtBeforeAndBookingStatusInAndIsDeletedFalseOrderByRemainingDueAtAsc(
                        com.sep.treksphere.enums.booking.PaymentPlan paymentPlan,
                        PaymentStatus paymentStatus,
                        LocalDateTime now,
                        java.util.Collection<BookingStatus> bookingStatuses);

        List<Booking> findBySchedule_ScheduleIdAndBookingStatusAndIsDeletedFalse(UUID scheduleId, BookingStatus status);

        Optional<Booking> findByUser_UserIdAndBookingRequestKeyAndIsDeletedFalse(UUID userId, String bookingRequestKey);
}
