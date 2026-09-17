package com.sep.treksphere.tour;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.tour.checkpoint.TourCheckpointRepository;
import com.sep.treksphere.tour.schedule.ScheduleStatus;
import com.sep.treksphere.tour.schedule.TourScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void validatePublishedStructure_ValidTour_DoesNotThrow() {
        Tour tour = validTour();
        when(checkpointRepository.countByTourAndIsDeletedFalse(tour)).thenReturn(2L);

        assertDoesNotThrow(() -> service.validatePublishedStructure(tour));
    }

    @Test
    void validatePublishedStructure_MissingCoverImage_ThrowsAppException() {
        Tour tour = validTour();
        tour.setCoverImageUrl(null);
        when(checkpointRepository.countByTourAndIsDeletedFalse(tour)).thenReturn(2L);

        assertThrows(AppException.class, () -> service.validatePublishedStructure(tour));
    }

    @Test
    void ensureCanRemoveCheckpoint_PublishedWithOnlyTwoCheckpoints_ThrowsAppException() {
        Tour tour = validTour();
        tour.setStatus(TourStatus.PUBLISHED);
        when(checkpointRepository.countByTourAndIsDeletedFalse(tour)).thenReturn(2L);

        assertThrows(AppException.class, () -> service.ensureCanRemoveCheckpoint(tour));
    }

    @Test
    void ensureCanRemoveCheckpoint_PublishedWithMoreThanTwoCheckpoints_DoesNotThrow() {
        Tour tour = validTour();
        tour.setStatus(TourStatus.PUBLISHED);
        when(checkpointRepository.countByTourAndIsDeletedFalse(tour)).thenReturn(3L);

        assertDoesNotThrow(() -> service.ensureCanRemoveCheckpoint(tour));
    }

    @Test
    void ensureCanRemoveCheckpoint_DraftTour_DoesNotThrowRegardlessOfCount() {
        Tour tour = validTour();
        tour.setStatus(TourStatus.DRAFT);

        assertDoesNotThrow(() -> service.ensureCanRemoveCheckpoint(tour));
        verify(checkpointRepository, never()).countByTourAndIsDeletedFalse(any());
    }

    @Test
    void ensureCanRemoveFutureOpenSchedule_NotPublished_DoesNotThrow() {
        Tour tour = validTour();
        tour.setStatus(TourStatus.DRAFT);

        assertDoesNotThrow(() -> service.ensureCanRemoveFutureOpenSchedule(tour, UUID.randomUUID(), true));
        verify(scheduleRepository, never()).countFutureOpenSchedulesExcluding(any(), any(), any());
    }

    @Test
    void ensureCanRemoveFutureOpenSchedule_PublishedButNotQualifyingSchedule_DoesNotThrow() {
        Tour tour = validTour();
        tour.setStatus(TourStatus.PUBLISHED);

        assertDoesNotThrow(() -> service.ensureCanRemoveFutureOpenSchedule(tour, UUID.randomUUID(), false));
        verify(scheduleRepository, never()).countFutureOpenSchedulesExcluding(any(), any(), any());
    }

    @Test
    void ensureCanRemoveFutureOpenSchedule_PublishedQualifyingWithRemaining_DoesNotThrow() {
        Tour tour = validTour();
        tour.setTourId(UUID.randomUUID());
        tour.setStatus(TourStatus.PUBLISHED);
        UUID scheduleId = UUID.randomUUID();
        when(scheduleRepository.countFutureOpenSchedulesExcluding(
                eq(tour.getTourId()), any(LocalDate.class), eq(scheduleId))).thenReturn(1L);

        assertDoesNotThrow(() -> service.ensureCanRemoveFutureOpenSchedule(tour, scheduleId, true));
    }

    @Test
    void ensureCanRemoveFutureOpenSchedule_PublishedQualifyingNoneRemaining_ThrowsAppException() {
        Tour tour = validTour();
        tour.setTourId(UUID.randomUUID());
        tour.setStatus(TourStatus.PUBLISHED);
        UUID scheduleId = UUID.randomUUID();
        when(scheduleRepository.countFutureOpenSchedulesExcluding(
                eq(tour.getTourId()), any(LocalDate.class), eq(scheduleId))).thenReturn(0L);

        assertThrows(AppException.class,
                () -> service.ensureCanRemoveFutureOpenSchedule(tour, scheduleId, true));
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
