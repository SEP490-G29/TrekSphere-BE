package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourScheduleRepository extends JpaRepository<TourSchedule, UUID> {
    List<TourSchedule> findByTourAndStatusOrderByDepartureDateAsc(Tour tour, ScheduleStatus status);
    List<TourSchedule> findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(Tour tour, ScheduleStatus status, LocalDate date);
    Optional<TourSchedule> findByScheduleIdAndIsDeletedFalse(UUID scheduleId);
}

