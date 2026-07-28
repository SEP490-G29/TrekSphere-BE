package com.sep.treksphere.repository;

import com.sep.treksphere.entity.PorterSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PorterScheduleRepository extends JpaRepository<PorterSchedule, UUID> {

    boolean existsByTourSession_TourSessionIdAndPorter_PorterIdAndIsDeletedFalse(UUID sessionId, UUID porterId);

    List<PorterSchedule> findByTourSession_TourSessionIdAndIsDeletedFalse(UUID sessionId);

    @Query("SELECT COUNT(ps) FROM PorterSchedule ps " +
            "WHERE ps.porter.porterId = :porterId " +
            "AND ps.isDeleted = false " +
            "AND ps.tourSession.status NOT IN ('COMPLETED', 'CANCELLED') " +
            "AND ps.tourSession.tourSchedule.departureDate <= :returnDate " +
            "AND ps.tourSession.tourSchedule.returnDate >= :departureDate")
    int countOverlappingSchedules(
            @Param("porterId") UUID porterId,
            @Param("departureDate") LocalDate departureDate,
            @Param("returnDate") LocalDate returnDate
    );
}
