package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.VendorDashboardFilterRequest;
import com.sep.treksphere.dto.response.dashboard.*;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentStatus;
import com.sep.treksphere.enums.dashboard.GroupBy;
import com.sep.treksphere.enums.dashboard.ScheduleRiskStatus;
import com.sep.treksphere.enums.dashboard.TimeRange;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorDashboardServiceImplTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorStaffRepository vendorStaffRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TourScheduleRepository tourScheduleRepository;

    @InjectMocks
    private VendorDashboardServiceImpl dashboardService;

    private Vendor vendor;
    private User manager;
    private Tour tour;
    private TourSchedule schedule;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setUserId(UUID.randomUUID());
        manager.setEmail("vendor@treksphere.com");

        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setCompanyName("Trek Expedition");
        vendor.setManager(manager);

        tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setTourName("Tà Năng - Phan Dũng");
        tour.setVendor(vendor);
        tour.setMinCapacity(8);
        tour.setMaxCapacity(20);

        schedule = new TourSchedule();
        schedule.setScheduleId(UUID.randomUUID());
        schedule.setTour(tour);
        schedule.setDepartureDate(LocalDate.now().plusDays(5));
        schedule.setReturnDate(LocalDate.now().plusDays(7));
        schedule.setBookedSlots(4);
    }

    @Test
    void getOverview_Success() {
        String email = "vendor@treksphere.com";
        VendorDashboardFilterRequest filterRequest = new VendorDashboardFilterRequest();
        filterRequest.setTimeRange(TimeRange.LAST_30_DAYS);

        Booking booking1 = new Booking();
        booking1.setBookingId(UUID.randomUUID());
        booking1.setTotalPrice(BigDecimal.valueOf(2500000));
        booking1.setNumberOfParticipants(2);
        booking1.setBookingStatus(BookingStatus.CONFIRMED);
        booking1.setPaymentStatus(PaymentStatus.PAID);

        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.of(vendor));
        when(bookingRepository.findVendorBookingsInPeriod(eq(vendor.getVendorId()), any(), any()))
                .thenReturn(List.of(booking1))
                .thenReturn(Collections.emptyList());

        when(tourScheduleRepository.findVendorSchedulesInPeriod(eq(vendor.getVendorId()), any(), any()))
                .thenReturn(List.of(schedule));

        VendorDashboardOverviewResponse response = dashboardService.getOverview(email, filterRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualTo(BigDecimal.valueOf(2500000));
        assertThat(response.getTotalTravelers()).isEqualTo(2L);
        assertThat(response.getAvgOccupancyRate()).isEqualTo(20.0);
    }

    @Test
    void getRevenueChart_Success() {
        String email = "vendor@treksphere.com";
        VendorDashboardFilterRequest filterRequest = new VendorDashboardFilterRequest();
        filterRequest.setTimeRange(TimeRange.LAST_30_DAYS);
        filterRequest.setGroupBy(GroupBy.DAY);

        Booking booking1 = new Booking();
        booking1.setBookingId(UUID.randomUUID());
        booking1.setTotalPrice(BigDecimal.valueOf(1500000));
        booking1.setBookingStatus(BookingStatus.CONFIRMED);
        booking1.setPaymentStatus(PaymentStatus.PAID);
        booking1.setCreatedAt(LocalDateTime.now());

        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.of(vendor));
        when(bookingRepository.findVendorBookingsInPeriod(eq(vendor.getVendorId()), any(), any()))
                .thenReturn(List.of(booking1));

        RevenueChartResponse response = dashboardService.getRevenueChart(email, filterRequest);

        assertThat(response).isNotNull();
        assertThat(response.getGroupBy()).isEqualTo(GroupBy.DAY);
        assertThat(response.getChartData()).isNotEmpty();
    }

    @Test
    void getTopTours_Success() {
        String email = "vendor@treksphere.com";
        VendorDashboardFilterRequest filterRequest = new VendorDashboardFilterRequest();

        Booking booking1 = new Booking();
        booking1.setSchedule(schedule);
        booking1.setNumberOfParticipants(4);
        booking1.setTotalPrice(BigDecimal.valueOf(5000000));
        booking1.setBookingStatus(BookingStatus.CONFIRMED);
        booking1.setPaymentStatus(PaymentStatus.PAID);

        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.of(vendor));
        when(bookingRepository.findVendorBookingsInPeriod(eq(vendor.getVendorId()), any(), any()))
                .thenReturn(List.of(booking1));

        List<TopSellingTourResponse> topTours = dashboardService.getTopTours(email, filterRequest, 5);

        assertThat(topTours).hasSize(1);
        assertThat(topTours.get(0).getTourName()).isEqualTo("Tà Năng - Phan Dũng");
        assertThat(topTours.get(0).getTotalTravelers()).isEqualTo(4L);
    }

    @Test
    void getUpcomingSchedules_RiskStatus_Danger() {
        String email = "vendor@treksphere.com";
        // 5 days left (< 7 days) and bookedSlots = 4 < minCapacity = 8 -> DANGER
        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.of(vendor));
        when(tourScheduleRepository.findVendorUpcomingSchedules(eq(vendor.getVendorId()), any()))
                .thenReturn(List.of(schedule));

        List<UpcomingScheduleResponse> upcoming = dashboardService.getUpcomingSchedules(email, 10, null);

        assertThat(upcoming).hasSize(1);
        assertThat(upcoming.get(0).getRiskStatus()).isEqualTo(ScheduleRiskStatus.DANGER);
        assertThat(upcoming.get(0).getStatusColor()).isEqualTo("RED");
    }

    @Test
    void getUnderCapacityAlerts_Success() {
        String email = "vendor@treksphere.com";
        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.of(vendor));
        when(tourScheduleRepository.findVendorUpcomingSchedules(eq(vendor.getVendorId()), any()))
                .thenReturn(List.of(schedule));

        List<UnderCapacityAlertResponse> alerts = dashboardService.getUnderCapacityAlerts(email, 7);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getMissingSlots()).isEqualTo(4); // 8 min - 4 booked
        assertThat(alerts.get(0).getAlertMessage()).contains("Tà Năng - Phan Dũng");
    }

    @Test
    void getScheduleManifest_Success() {
        String email = "vendor@treksphere.com";
        UUID scheduleId = schedule.getScheduleId();

        User booker = new User();
        booker.setFullName("Nguyen Van B");
        booker.setEmail("b@gmail.com");
        booker.setPhone("0987654321");

        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setBookingCode("BK-101");
        booking.setUser(booker);
        booking.setSchedule(schedule);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.of(vendor));
        when(tourScheduleRepository.findByScheduleIdAndIsDeletedFalse(scheduleId)).thenReturn(Optional.of(schedule));
        when(bookingRepository.findByScheduleId(scheduleId)).thenReturn(List.of(booking));

        ScheduleManifestResponse manifest = dashboardService.getScheduleManifest(email, scheduleId);

        assertThat(manifest).isNotNull();
        assertThat(manifest.getTourName()).isEqualTo("Tà Năng - Phan Dũng");
        assertThat(manifest.getParticipants()).hasSize(1);
        assertThat(manifest.getParticipants().get(0).getBookerName()).isEqualTo("Nguyen Van B");
    }

    @Test
    void getOverview_UserNotFound_ThrowsException() {
        String email = "unknown@treksphere.com";
        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.empty());
        when(vendorStaffRepository.findByUser_Email(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getOverview(email, new VendorDashboardFilterRequest()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }
}
