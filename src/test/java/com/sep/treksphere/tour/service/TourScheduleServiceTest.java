package com.sep.treksphere.tour.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.notification.service.NotificationService;
import com.sep.treksphere.tour.dto.request.CreateScheduleRequest;
import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.repository.TourRepository;
import com.sep.treksphere.tour.repository.TourScheduleRepository;
import com.sep.treksphere.vendor.entity.Vendor;
import com.sep.treksphere.vendor.service.VendorAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourScheduleServiceTest {

    private static final String MANAGER_EMAIL = "vendor@example.com";

    private TourScheduleRepository tourScheduleRepository;
    private TourRepository tourRepository;
    private VendorAccessService vendorAccessService;
    private TourScheduleService service;

    private Tour tour;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        tourScheduleRepository = mock(TourScheduleRepository.class);
        tourRepository = mock(TourRepository.class);
        vendorAccessService = mock(VendorAccessService.class);
        TourReadinessService readinessService = mock(TourReadinessService.class);
        MatchingMemberRepository matchingMemberRepository = mock(MatchingMemberRepository.class);
        NotificationService notificationService = mock(NotificationService.class);

        service = new TourScheduleService(
                tourScheduleRepository, tourRepository, vendorAccessService,
                readinessService, matchingMemberRepository, notificationService);

        UUID vendorId = UUID.randomUUID();
        vendor = new Vendor();
        vendor.setVendorId(vendorId);

        tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setVendor(vendor);
        tour.setDurationDays(3);

        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(MANAGER_EMAIL)).thenReturn(vendor);
    }

    private CreateScheduleRequest request(LocalDate departure, LocalDate returnDate) {
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDepartureDate(departure);
        request.setReturnDate(returnDate);
        return request;
    }

    @Test
    void rejectsScheduleShorterThanTourDuration() {
        LocalDate departure = LocalDate.now().plusDays(10);
        // Tour requires 3 days (departure + 2), but this only spans 1 day.
        CreateScheduleRequest request = request(departure, departure);

        AppException ex = assertThrows(AppException.class,
                () -> service.createSchedule(MANAGER_EMAIL, tour.getTourId(), request));
        assertEquals(ErrorCode.SCHEDULE_DATES_NOT_MATCH_TOUR_DURATION, ex.getErrorCode());
    }

    @Test
    void rejectsScheduleLongerThanTourDuration() {
        LocalDate departure = LocalDate.now().plusDays(10);
        // Tour requires 3 days (departure + 2), but this spans 4 days.
        CreateScheduleRequest request = request(departure, departure.plusDays(3));

        AppException ex = assertThrows(AppException.class,
                () -> service.createSchedule(MANAGER_EMAIL, tour.getTourId(), request));
        assertEquals(ErrorCode.SCHEDULE_DATES_NOT_MATCH_TOUR_DURATION, ex.getErrorCode());
    }

    @Test
    void rejectsDuplicateScheduleWithSameDates() {
        LocalDate departure = LocalDate.now().plusDays(10);
        LocalDate returnDate = departure.plusDays(2);
        CreateScheduleRequest request = request(departure, returnDate);

        when(tourScheduleRepository.existsDuplicateSchedule(
                eq(tour.getTourId()), eq(departure), eq(returnDate), isNull()))
                .thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> service.createSchedule(MANAGER_EMAIL, tour.getTourId(), request));
        assertEquals(ErrorCode.SCHEDULE_DUPLICATE_DATES, ex.getErrorCode());
    }

    @Test
    void acceptsScheduleMatchingTourDurationWithNoDuplicate() {
        LocalDate departure = LocalDate.now().plusDays(10);
        LocalDate returnDate = departure.plusDays(2);
        CreateScheduleRequest request = request(departure, returnDate);

        when(tourScheduleRepository.existsDuplicateSchedule(
                eq(tour.getTourId()), eq(departure), eq(returnDate), isNull()))
                .thenReturn(false);
        when(tourScheduleRepository.save(any())).thenAnswer(invocation -> {
            com.sep.treksphere.tour.entity.TourSchedule saved = invocation.getArgument(0);
            saved.setTourScheduleId(UUID.randomUUID());
            return saved;
        });

        assertDoesNotThrow(() -> service.createSchedule(MANAGER_EMAIL, tour.getTourId(), request));
    }
}
