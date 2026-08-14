package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.CreateScheduleRequest;
import com.sep.treksphere.dto.request.UpdateScheduleRequest;
import com.sep.treksphere.dto.request.VendorBookingCancelRequest;
import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.RefundReason;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.TourSessionRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.CancellationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourScheduleServiceImplTest {

    @Mock private TourScheduleRepository tourScheduleRepository;
    @Mock private TourSessionRepository tourSessionRepository;
    @Mock private TourRepository tourRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private VendorStaffRepository vendorStaffRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private CancellationService cancellationService;

    @InjectMocks private TourScheduleServiceImpl service;

    private final String vendorEmail = "vendor@example.com";
    private TourSchedule schedule;
    private Vendor vendor;
    private Booking booking;

    @BeforeEach
    void setUp() {
        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());

        Tour tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setTourName("Trek test");
        tour.setVendor(vendor);
        tour.setDurationDays(3);

        schedule = new TourSchedule();
        schedule.setScheduleId(UUID.randomUUID());
        schedule.setTour(tour);
        schedule.setDepartureDate(LocalDate.now().plusDays(10));
        schedule.setReturnDate(LocalDate.now().plusDays(12));
        schedule.setPrice(new BigDecimal("1000000"));
        schedule.setAvailableSlots(8);
        schedule.setBookedSlots(2);
        schedule.setStatus(ScheduleStatus.OPEN);
        schedule.setIsDeleted(false);

        User trekker = new User();
        trekker.setUserId(UUID.randomUUID());

        booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setSchedule(schedule);
        booking.setUser(trekker);
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        lenient().when(tourScheduleRepository.findByIdForUpdate(schedule.getScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(vendorRepository.findByManager_Email(vendorEmail)).thenReturn(Optional.of(vendor));
    }

    @Test
    void createScheduleRejectsDateRangeLongerThanTourDuration() {
        Tour tour = schedule.getTour();
        when(tourRepository.findById(tour.getTourId())).thenReturn(Optional.of(tour));

        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDepartureDate(LocalDate.now().plusDays(10));
        request.setReturnDate(LocalDate.now().plusDays(16));
        request.setPrice(new BigDecimal("1000000"));
        request.setAvailableSlots(8);

        AppException exception = assertThrows(AppException.class,
                () -> service.createSchedule(vendorEmail, tour.getTourId(), request));

        assertEquals(com.sep.treksphere.exception.ErrorCode.SCHEDULE_DURATION_EXCEEDS_TOUR,
                exception.getErrorCode());
        verify(tourScheduleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteScheduleSoftDeletesScheduleAndLinkedSession() {
        schedule.setBookedSlots(0);
        TourSession session = new TourSession();
        session.setTourSchedule(schedule);
        session.setIsDeleted(false);
        when(tourScheduleRepository.findByScheduleIdAndIsDeletedFalse(schedule.getScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(bookingRepository.existsByScheduleAndBookingStatusInAndIsDeletedFalse(
                eq(schedule), anyCollection())).thenReturn(false);
        when(tourSessionRepository.findByTourSchedule_ScheduleIdAndIsDeletedFalse(
                schedule.getScheduleId())).thenReturn(Optional.of(session));

        service.deleteSchedule(vendorEmail, schedule.getScheduleId());

        assertTrue(schedule.getIsDeleted());
        assertTrue(session.getIsDeleted());
        verify(tourScheduleRepository).save(schedule);
        verify(tourSessionRepository).save(session);
    }

    @Test
    void deleteScheduleRejectsScheduleWithActiveBookings() {
        when(tourScheduleRepository.findByScheduleIdAndIsDeletedFalse(schedule.getScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(bookingRepository.existsByScheduleAndBookingStatusInAndIsDeletedFalse(
                eq(schedule), anyCollection())).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> service.deleteSchedule(vendorEmail, schedule.getScheduleId()));

        assertEquals(com.sep.treksphere.exception.ErrorCode.SCHEDULE_HAS_BOOKINGS,
                exception.getErrorCode());
        verify(tourScheduleRepository, never()).save(schedule);
    }

    @Test
    void cancelScheduleDelegatesBookingCancellationAndNotification() {
        when(bookingRepository.findByScheduleId(schedule.getScheduleId())).thenReturn(List.of(booking));
        when(tourScheduleRepository.save(schedule)).thenReturn(schedule);

        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setStatus(ScheduleStatus.CANCELLED);
        request.setReason("Thời tiết nguy hiểm");

        service.updateSchedule(vendorEmail, schedule.getScheduleId(), request);

        ArgumentCaptor<VendorBookingCancelRequest> cancelRequest =
                ArgumentCaptor.forClass(VendorBookingCancelRequest.class);
        verify(cancellationService).cancelByVendor(
                eq(vendorEmail), eq(booking.getBookingId()), cancelRequest.capture());
        assertEquals(RefundReason.VENDOR_CANCEL, cancelRequest.getValue().getReason());
        assertEquals("Thời tiết nguy hiểm", cancelRequest.getValue().getReasonDetail());
        assertEquals(ScheduleStatus.CANCELLED, schedule.getStatus());

        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelScheduleRejectsBookingsThatAlreadyStarted() {
        booking.setBookingStatus(BookingStatus.IN_PROGRESS);
        when(bookingRepository.findByScheduleId(schedule.getScheduleId())).thenReturn(List.of(booking));

        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setStatus(ScheduleStatus.CANCELLED);
        request.setReason("Thời tiết nguy hiểm");

        assertThrows(AppException.class,
                () -> service.updateSchedule(vendorEmail, schedule.getScheduleId(), request));

        verify(cancellationService, never()).cancelByVendor(
                eq(vendorEmail), eq(booking.getBookingId()), org.mockito.ArgumentMatchers.any());
        verify(tourScheduleRepository, never()).save(schedule);
    }
}
