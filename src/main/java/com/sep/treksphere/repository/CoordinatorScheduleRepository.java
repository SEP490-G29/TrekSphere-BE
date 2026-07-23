package com.sep.treksphere.repository;

import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CoordinatorScheduleRepository extends JpaRepository<CoordinatorSchedule, UUID> {

    @Query("SELECT COUNT(c) FROM CoordinatorSchedule c " +
            "JOIN c.tourSession ts " +
            "JOIN ts.tourSchedule sch " +
            "WHERE c.coordinator.id = :coordinatorId " +
            "AND c.isDeleted = false " +
            "AND ts.isDeleted = false " +
            "AND sch.isDeleted = false " +
            "AND sch.departureDate <= :newReturnDate " +
            "AND sch.returnDate >= :newDepartureDate")
    long countOverlappingSchedules(@Param("coordinatorId") UUID coordinatorId,
                                   @Param("newDepartureDate") LocalDate newDepartureDate,
                                   @Param("newReturnDate") LocalDate newReturnDate);

    @Query("SELECT COUNT(c) FROM CoordinatorSchedule c " +
            "JOIN c.tourSession ts " +
            "WHERE c.coordinator.id = :coordinatorId " +
            "AND c.isDeleted = false " +
            "AND ts.isDeleted = false " +
            "AND ts.status = :status")
    long countSchedulesByStatus(@Param("coordinatorId") UUID coordinatorId,
                                @Param("status") TourSessionStatus status);

    List<CoordinatorSchedule> findByTourSession_TourSessionIdAndIsDeletedFalse(UUID sessionId);
}
