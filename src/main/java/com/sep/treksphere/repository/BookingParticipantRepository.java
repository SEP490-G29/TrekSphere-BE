package com.sep.treksphere.repository;

import com.sep.treksphere.entity.BookingParticipant;
import com.sep.treksphere.enums.booking.BookingStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingParticipantRepository extends JpaRepository<BookingParticipant, UUID> {

    @Query("SELECT bp FROM BookingParticipant bp JOIN bp.booking b " +
           "WHERE b.schedule.scheduleId = :scheduleId " +
           "AND b.bookingStatus = :bookingStatus " +
           "AND b.isDeleted = false " +
           "AND bp.isDeleted = false")
    List<BookingParticipant> findActiveParticipantsByScheduleId(
            @Param("scheduleId") UUID scheduleId,
            @Param("bookingStatus") BookingStatus bookingStatus
    );

    boolean existsByEmailAndBooking_Schedule_ScheduleIdAndBooking_BookingStatusAndIsDeletedFalse(
            String email, UUID scheduleId, BookingStatus bookingStatus
    );

    boolean existsByPhoneAndBooking_Schedule_ScheduleIdAndBooking_BookingStatusAndIsDeletedFalse(
            String phone, UUID scheduleId, BookingStatus bookingStatus
    );

    @Query("SELECT bp FROM BookingParticipant bp JOIN bp.booking b JOIN b.schedule s, TourSession ts " +
           "WHERE ts.tourSchedule = s AND ts.tourSessionId = :tourSessionId " +
           "AND b.bookingStatus IN :bookingStatuses " +
           "AND bp.isDeleted = false " +
           "AND (CAST(:keyword AS String) IS NULL OR " +
           "LOWER(bp.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')) OR " +
           "LOWER(bp.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')) OR " +
           "LOWER(bp.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')) OR " +
           "LOWER(bp.idNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))) " +
           "AND (:isPresentStart IS NULL OR bp.isPresentStart = :isPresentStart) " +
           "AND (:isPresentEnd IS NULL OR bp.isPresentEnd = :isPresentEnd)")
    Page<BookingParticipant> findParticipantsByTourSessionIdAndFilters(
            @Param("tourSessionId") UUID tourSessionId,
            @Param("bookingStatuses") List<BookingStatus> bookingStatuses,
            @Param("keyword") String keyword,
            @Param("isPresentStart") Boolean isPresentStart,
            @Param("isPresentEnd") Boolean isPresentEnd,
            Pageable pageable
    );
}
