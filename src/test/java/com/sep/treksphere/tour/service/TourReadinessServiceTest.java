package com.sep.treksphere.tour.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.enums.DifficultyLevel;
import com.sep.treksphere.tour.enums.TourStatus;
import com.sep.treksphere.tour.repository.TourCheckpointRepository;
import com.sep.treksphere.tour.enums.ScheduleStatus;
import com.sep.treksphere.tour.repository.TourScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourReadinessServiceTest {

    private TourCheckpointRepository checkpointRepository;
    private TourScheduleRepository scheduleRepository;
    private TourReadinessService service;

    @BeforeEach
    void setUp() {
        checkpointRepository = mock(TourCheckpointRepository.class);
        scheduleRepository = mock(TourScheduleRepository.class);
        service = new TourReadinessService(checkpointRepository, scheduleRepository);
    }

    @Test
    void completeTourCanBePublished() {
        Tour tour = validTour();
        when(checkpointRepository.countByTourAndIsDeletedFalse(tour)).thenReturn(2L);
        when(scheduleRepository.existsByTourAndStatusAndDepartureDateAfterAndIsDeletedFalse(
                eq(tour), eq(ScheduleStatus.OPEN), any(LocalDate.class))).thenReturn(true);

        assertEquals(List.of(), service.getPublishReadinessErrors(tour));
        assertDoesNotThrow(() -> service.validateForPublish(tour));
    }

    @Test
    void publishRequiresCapacityCheckpointsAndFutureOpenSchedule() {
        Tour tour = validTour();
        tour.setMinCapacity(10);
        tour.setMaxCapacity(5);
        when(checkpointRepository.countByTourAndIsDeletedFalse(tour)).thenReturn(1L);
        when(scheduleRepository.existsByTourAndStatusAndDepartureDateAfterAndIsDeletedFalse(
                eq(tour), eq(ScheduleStatus.OPEN), any(LocalDate.class))).thenReturn(false);

        assertEquals(
                List.of("INVALID_CAPACITY", "INSUFFICIENT_CHECKPOINTS", "NO_FUTURE_OPEN_SCHEDULE"),
                service.getPublishReadinessErrors(tour));
        assertThrows(AppException.class, () -> service.validateForPublish(tour));
    }

    private Tour validTour() {
        Tour tour = new Tour();
        tour.setTourName("Fansipan");
        tour.setDescription("Mô tả");
        tour.setDifficulty(DifficultyLevel.MODERATE);
        tour.setLocation("Sa Pa");
        tour.setDurationDays(2);
        tour.setMinCapacity(4);
        tour.setMaxCapacity(12);
        tour.setCoverImageUrl("https://example.com/cover.jpg");
        tour.setStatus(TourStatus.DRAFT);
        return tour;
    }
}
