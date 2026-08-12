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
import com.sep.treksphere.service.VendorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorDashboardServiceImpl implements VendorDashboardService {

    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final BookingRepository bookingRepository;
    private final TourScheduleRepository tourScheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public VendorDashboardOverviewResponse getOverview(String email, VendorDashboardFilterRequest request) {
        Vendor vendor = getAssociatedVendor(email);
        DateRange currentRange = resolveDateRange(request.getTimeRange(), request.getStartDate(), request.getEndDate());
        DateRange previousRange = getPreviousPeriodDateRange(currentRange);

        List<Booking> currentBookings = bookingRepository.findVendorBookingsInPeriod(
                vendor.getVendorId(), currentRange.getStart(), currentRange.getEnd()
        );
        List<Booking> previousBookings = bookingRepository.findVendorBookingsInPeriod(
                vendor.getVendorId(), previousRange.getStart(), previousRange.getEnd()
        );

        // 1. Total Revenue
        BigDecimal currentRevenue = calculateTotalRevenue(currentBookings);
        BigDecimal previousRevenue = calculateTotalRevenue(previousBookings);
        Double revenueChange = calculatePercentageChange(currentRevenue.doubleValue(), previousRevenue.doubleValue());

        // 2. Total Travelers
        long currentTravelers = calculateTotalTravelers(currentBookings);
        long previousTravelers = calculateTotalTravelers(previousBookings);
        Double travelersChange = calculatePercentageChange((double) currentTravelers, (double) previousTravelers);

        // 3. Average Occupancy Rate
        List<TourSchedule> currentSchedules = tourScheduleRepository.findVendorSchedulesInPeriod(
                vendor.getVendorId(), currentRange.getStart(), currentRange.getEnd()
        );
        List<TourSchedule> previousSchedules = tourScheduleRepository.findVendorSchedulesInPeriod(
                vendor.getVendorId(), previousRange.getStart(), previousRange.getEnd()
        );

        Double currentOccupancy = calculateAverageOccupancy(currentSchedules);
        Double previousOccupancy = calculateAverageOccupancy(previousSchedules);
        Double occupancyChange = calculatePercentageChange(currentOccupancy, previousOccupancy);

        // 4. Cancellation Rate
        Double currentCancelRate = calculateCancellationRate(currentBookings);
        Double previousCancelRate = calculateCancellationRate(previousBookings);
        Double cancelRateChange = calculatePercentageChange(currentCancelRate, previousCancelRate);

        return VendorDashboardOverviewResponse.builder()
                .totalRevenue(currentRevenue)
                .revenueChangePercentage(revenueChange)
                .totalTravelers(currentTravelers)
                .travelersChangePercentage(travelersChange)
                .avgOccupancyRate(currentOccupancy)
                .occupancyRateChangePercentage(occupancyChange)
                .cancellationRate(currentCancelRate)
                .cancellationRateChangePercentage(cancelRateChange)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueChartResponse getRevenueChart(String email, VendorDashboardFilterRequest request) {
        Vendor vendor = getAssociatedVendor(email);
        DateRange dateRange = resolveDateRange(request.getTimeRange(), request.getStartDate(), request.getEndDate());
        GroupBy groupBy = request.getGroupBy() != null ? request.getGroupBy() : GroupBy.DAY;

        List<Booking> validBookings = bookingRepository.findVendorBookingsInPeriod(
                vendor.getVendorId(), dateRange.getStart(), dateRange.getEnd()
        ).stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getPaymentStatus() != PaymentStatus.UNPAID)
                .toList();

        Map<String, BigDecimal> revenueMap = new LinkedHashMap<>();
        Map<String, Long> bookingCountMap = new LinkedHashMap<>();

        DateTimeFormatter formatter = groupBy == GroupBy.MONTH
                ? DateTimeFormatter.ofPattern("MM/yyyy")
                : DateTimeFormatter.ofPattern("dd/MM");

        if (groupBy == GroupBy.DAY) {
            LocalDate start = dateRange.getStart().toLocalDate();
            LocalDate end = dateRange.getEnd().toLocalDate();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                String key = date.format(formatter);
                revenueMap.put(key, BigDecimal.ZERO);
                bookingCountMap.put(key, 0L);
            }
        }

        for (Booking booking : validBookings) {
            String key = booking.getCreatedAt().format(formatter);
            revenueMap.put(key, revenueMap.getOrDefault(key, BigDecimal.ZERO).add(booking.getTotalPrice()));
            bookingCountMap.put(key, bookingCountMap.getOrDefault(key, 0L) + 1);
        }

        List<RevenueChartPointResponse> chartData = new ArrayList<>();
        for (String key : revenueMap.keySet()) {
            chartData.add(RevenueChartPointResponse.builder()
                    .label(key)
                    .revenue(revenueMap.get(key))
                    .totalBookings(bookingCountMap.get(key))
                    .build());
        }

        return RevenueChartResponse.builder()
                .groupBy(groupBy)
                .chartData(chartData)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopSellingTourResponse> getTopTours(String email, VendorDashboardFilterRequest request, int limit) {
        Vendor vendor = getAssociatedVendor(email);
        DateRange dateRange = resolveDateRange(request.getTimeRange(), request.getStartDate(), request.getEndDate());

        List<Booking> bookings = bookingRepository.findVendorBookingsInPeriod(
                vendor.getVendorId(), dateRange.getStart(), dateRange.getEnd()
        ).stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .toList();

        Map<Tour, List<Booking>> tourBookingsMap = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getSchedule().getTour()));

        List<TopSellingTourResponse> topTours = new ArrayList<>();
        for (Map.Entry<Tour, List<Booking>> entry : tourBookingsMap.entrySet()) {
            Tour tour = entry.getKey();
            List<Booking> tourBookings = entry.getValue();

            long totalTravelers = tourBookings.stream()
                    .mapToLong(Booking::getNumberOfParticipants)
                    .sum();

            BigDecimal totalRevenue = tourBookings.stream()
                    .filter(b -> b.getPaymentStatus() != PaymentStatus.UNPAID)
                    .map(Booking::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            topTours.add(TopSellingTourResponse.builder()
                    .tourId(tour.getTourId())
                    .tourName(tour.getTourName())
                    .totalTravelers(totalTravelers)
                    .totalRevenue(totalRevenue)
                    .build());
        }

        topTours.sort(Comparator.comparingLong(TopSellingTourResponse::getTotalTravelers).reversed());
        return topTours.stream().limit(limit > 0 ? limit : 5).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingScheduleResponse> getUpcomingSchedules(String email, int limit, Integer daysAhead) {
        Vendor vendor = getAssociatedVendor(email);
        LocalDate today = LocalDate.now();
        List<TourSchedule> upcomingSchedules = tourScheduleRepository.findVendorUpcomingSchedules(vendor.getVendorId(), today);

        if (daysAhead != null && daysAhead > 0) {
            LocalDate maxDate = today.plusDays(daysAhead);
            upcomingSchedules = upcomingSchedules.stream()
                    .filter(s -> !s.getDepartureDate().isAfter(maxDate))
                    .toList();
        }

        List<UpcomingScheduleResponse> responseList = new ArrayList<>();
        for (TourSchedule schedule : upcomingSchedules) {
            Tour tour = schedule.getTour();
            int booked = schedule.getBookedSlots();
            int maxCap = tour.getMaxCapacity();
            int minCap = tour.getMinCapacity();

            double occupancyRate = maxCap > 0
                    ? BigDecimal.valueOf((double) booked / maxCap * 100).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            long daysLeft = ChronoUnit.DAYS.between(today, schedule.getDepartureDate());
            ScheduleRiskStatus riskStatus;
            String statusColor;

            if (daysLeft <= 7 && booked < minCap) {
                riskStatus = ScheduleRiskStatus.DANGER;
                statusColor = "RED";
            } else if (booked < minCap || occupancyRate < 70.0) {
                riskStatus = ScheduleRiskStatus.WARNING;
                statusColor = "YELLOW";
            } else {
                riskStatus = ScheduleRiskStatus.SAFE;
                statusColor = "GREEN";
            }

            responseList.add(UpcomingScheduleResponse.builder()
                    .scheduleId(schedule.getScheduleId())
                    .tourId(tour.getTourId())
                    .tourName(tour.getTourName())
                    .departureDate(schedule.getDepartureDate())
                    .bookedSlots(booked)
                    .maxCapacity(maxCap)
                    .minCapacity(minCap)
                    .occupancyRate(occupancyRate)
                    .riskStatus(riskStatus)
                    .statusColor(statusColor)
                    .build());
        }

        return responseList.stream().limit(limit > 0 ? limit : 10).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnderCapacityAlertResponse> getUnderCapacityAlerts(String email, Integer alertDaysThreshold) {
        Vendor vendor = getAssociatedVendor(email);
        int daysThreshold = (alertDaysThreshold != null && alertDaysThreshold > 0) ? alertDaysThreshold : 7;
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(daysThreshold);

        List<TourSchedule> upcomingSchedules = tourScheduleRepository.findVendorUpcomingSchedules(vendor.getVendorId(), today);

        List<UnderCapacityAlertResponse> alerts = new ArrayList<>();
        for (TourSchedule schedule : upcomingSchedules) {
            if (schedule.getDepartureDate().isAfter(maxDate)) {
                continue;
            }

            Tour tour = schedule.getTour();
            int booked = schedule.getBookedSlots();
            int minCap = tour.getMinCapacity();

            if (booked < minCap) {
                long daysLeft = ChronoUnit.DAYS.between(today, schedule.getDepartureDate());
                int missing = minCap - booked;
                String alertMsg = String.format("Chuyến %s (%s) còn %d ngày khởi hành nhưng mới có %d/%d chỗ tối thiểu!",
                        tour.getTourName(), schedule.getDepartureDate().format(DateTimeFormatter.ofPattern("dd/MM")),
                        daysLeft, booked, minCap);

                alerts.add(UnderCapacityAlertResponse.builder()
                        .scheduleId(schedule.getScheduleId())
                        .tourName(tour.getTourName())
                        .departureDate(schedule.getDepartureDate())
                        .daysLeft(daysLeft)
                        .bookedSlots(booked)
                        .minCapacity(minCap)
                        .missingSlots(missing)
                        .alertMessage(alertMsg)
                        .build());
            }
        }

        return alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleManifestResponse getScheduleManifest(String email, UUID scheduleId) {
        Vendor vendor = getAssociatedVendor(email);

        TourSchedule schedule = tourScheduleRepository.findByScheduleIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (!schedule.getTour().getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        List<Booking> bookings = bookingRepository.findByScheduleId(scheduleId);

        List<ParticipantManifestResponse> participantList = new ArrayList<>();
        for (Booking booking : bookings) {
            User booker = booking.getUser();
            if (booking.getParticipants() != null && !booking.getParticipants().isEmpty()) {
                for (BookingParticipant p : booking.getParticipants()) {
                    participantList.add(ParticipantManifestResponse.builder()
                            .bookingId(booking.getBookingId())
                            .bookingCode(booking.getBookingCode())
                            .bookerName(booker != null ? booker.getFullName() : null)
                            .bookerPhone(booker != null ? booker.getPhone() : null)
                            .bookerEmail(booker != null ? booker.getEmail() : null)
                            .participantId(p.getParticipantId())
                            .fullName(p.getFullName())
                            .gender(p.getGender() != null ? p.getGender().name() : null)
                            .dateOfBirth(p.getDateOfBirth())
                            .phoneNumber(p.getPhone())
                            .specialNote(p.getSpecialRequirements())
                            .paymentStatus(booking.getPaymentStatus())
                            .bookingStatus(booking.getBookingStatus())
                            .build());
                }
            } else {
                // Booker is sole participant
                participantList.add(ParticipantManifestResponse.builder()
                        .bookingId(booking.getBookingId())
                        .bookingCode(booking.getBookingCode())
                        .bookerName(booker != null ? booker.getFullName() : null)
                        .bookerPhone(booker != null ? booker.getPhone() : null)
                        .bookerEmail(booker != null ? booker.getEmail() : null)
                        .fullName(booker != null ? booker.getFullName() : null)
                        .phoneNumber(booker != null ? booker.getPhone() : null)
                        .paymentStatus(booking.getPaymentStatus())
                        .bookingStatus(booking.getBookingStatus())
                        .build());
            }
        }

        Tour tour = schedule.getTour();
        return ScheduleManifestResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .tourName(tour.getTourName())
                .departureDate(schedule.getDepartureDate())
                .returnDate(schedule.getReturnDate())
                .bookedSlots(schedule.getBookedSlots())
                .maxCapacity(tour.getMaxCapacity())
                .minCapacity(tour.getMinCapacity())
                .participants(participantList)
                .build();
    }

    // Helper Methods
    private Vendor getAssociatedVendor(String email) {
        Optional<Vendor> vendorOpt = vendorRepository.findByManager_Email(email);
        if (vendorOpt.isPresent()) {
            return vendorOpt.get();
        }
        Optional<VendorStaff> staffOpt = vendorStaffRepository.findByUser_Email(email);
        if (staffOpt.isPresent()) {
            return staffOpt.get().getVendor();
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private DateRange resolveDateRange(TimeRange timeRange, LocalDate startDate, LocalDate endDate) {
        LocalDateTime now = LocalDateTime.now();
        if (timeRange == null) {
            timeRange = TimeRange.LAST_30_DAYS;
        }

        switch (timeRange) {
            case LAST_7_DAYS:
                return new DateRange(now.minusDays(7).with(LocalTime.MIN), now);
            case THIS_QUARTER:
                int currentMonth = now.getMonthValue();
                int firstMonthOfQuarter = ((currentMonth - 1) / 3) * 3 + 1;
                LocalDateTime startOfQuarter = LocalDateTime.of(now.getYear(), firstMonthOfQuarter, 1, 0, 0);
                return new DateRange(startOfQuarter, now);
            case CUSTOM:
                if (startDate != null && endDate != null) {
                    return new DateRange(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
                }
                return new DateRange(now.minusDays(30).with(LocalTime.MIN), now);
            case LAST_30_DAYS:
            default:
                return new DateRange(now.minusDays(30).with(LocalTime.MIN), now);
        }
    }

    private DateRange getPreviousPeriodDateRange(DateRange currentRange) {
        long daysDiff = ChronoUnit.DAYS.between(currentRange.getStart(), currentRange.getEnd());
        if (daysDiff <= 0) daysDiff = 30;
        return new DateRange(
                currentRange.getStart().minusDays(daysDiff),
                currentRange.getStart().minusNanos(1)
        );
    }

    private BigDecimal calculateTotalRevenue(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getPaymentStatus() != PaymentStatus.UNPAID)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long calculateTotalTravelers(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .mapToLong(Booking::getNumberOfParticipants)
                .sum();
    }

    private Double calculateAverageOccupancy(List<TourSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return 0.0;
        }
        int totalBooked = schedules.stream().mapToInt(TourSchedule::getBookedSlots).sum();
        int totalMaxCap = schedules.stream().mapToInt(s -> s.getTour().getMaxCapacity()).sum();
        if (totalMaxCap == 0) return 0.0;
        return BigDecimal.valueOf((double) totalBooked / totalMaxCap * 100)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private Double calculateCancellationRate(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return 0.0;
        }
        long cancelled = bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count();
        return BigDecimal.valueOf((double) cancelled / bookings.size() * 100)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private Double calculatePercentageChange(Double current, Double previous) {
        if (previous == null || previous == 0.0) {
            return current > 0 ? 100.0 : 0.0;
        }
        double change = ((current - previous) / previous) * 100.0;
        return BigDecimal.valueOf(change).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static class DateRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        public DateRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public LocalDateTime getEnd() {
            return end;
        }
    }
}
