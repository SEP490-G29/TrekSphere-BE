package com.sep.treksphere.repository;

import com.sep.treksphere.entity.BookingParticipant;
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
           "AND b.bookingStatus = 'CONFIRMED' " +
           "AND bp.isDeleted = false")
    List<BookingParticipant> findActiveParticipantsByScheduleId(@Param("scheduleId") UUID scheduleId);
}
