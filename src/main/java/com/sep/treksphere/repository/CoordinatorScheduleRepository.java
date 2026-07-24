package com.sep.treksphere.repository;

import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface CoordinatorScheduleRepository extends JpaRepository<CoordinatorSchedule, UUID> {

    @Query("SELECT cs FROM CoordinatorSchedule cs " +
           "JOIN cs.tourSession ts " +
           "JOIN ts.tourSchedule tsch " +
           "WHERE cs.coordinator.userId = :coordinatorId " +
           "AND cs.isDeleted = false " +
           "AND (:status IS NULL OR ts.status = :status) " +
           "AND (:isCancelled IS NULL OR cs.isCancelled = :isCancelled) " +
           "AND (:departureDateFrom IS NULL OR tsch.departureDate >= :departureDateFrom) " +
           "AND (:departureDateTo IS NULL OR tsch.departureDate <= :departureDateTo)")
    Page<CoordinatorSchedule> findByCoordinatorIdAndFilters(
            @Param("coordinatorId") UUID coordinatorId,
            @Param("status") TourSessionStatus status,
            @Param("isCancelled") Boolean isCancelled,
            @Param("departureDateFrom") LocalDate departureDateFrom,
            @Param("departureDateTo") LocalDate departureDateTo,
            Pageable pageable
    );
}
