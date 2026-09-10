package com.sep.treksphere.tour.schedule;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourReadinessService;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourScheduleService {

    private final TourScheduleRepository tourScheduleRepository;
    private final TourRepository tourRepository;
    private final VendorAccessService vendorAccessService;
    private final TourReadinessService readinessService;
    private final MatchingMemberRepository matchingMemberRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter VN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public List<TourScheduleResponse> getUpcomingSchedules(UUID tourId) {
        Tour tour = tourRepository.findPublishedDetailById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        return tourScheduleRepository
                .findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(
                        tour, ScheduleStatus.OPEN, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TourScheduleResponse createSchedule(String userEmail, UUID tourId, CreateScheduleRequest request) {
        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        validateTourBelongsToVendor(tour, vendor);
        validateDatesAndDuration(tour, request.getDepartureDate(), request.getReturnDate());

        TourSchedule schedule = new TourSchedule();
        schedule.setTour(tour);
        schedule.setDepartureDate(request.getDepartureDate());
        schedule.setReturnDate(request.getReturnDate());
        schedule.setPrice(request.getPrice());
        schedule.setStatus(ScheduleStatus.OPEN);
        return toResponse(tourScheduleRepository.save(schedule));
    }

    @Transactional
    public TourScheduleResponse updateSchedule(String userEmail, UUID scheduleId, UpdateScheduleRequest request) {
        TourSchedule schedule = tourScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        validateTourBelongsToVendor(schedule.getTour(), vendor);
        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_EDITABLE);
        }

        LocalDate today = LocalDate.now();
        LocalDate departure = request.getDepartureDate() == null
                ? schedule.getDepartureDate() : request.getDepartureDate();
        LocalDate returnDate = request.getReturnDate() == null
                ? schedule.getReturnDate() : request.getReturnDate();
        validateDatesAndDuration(schedule.getTour(), departure, returnDate);

        ScheduleStatus requestedStatus = request.getStatus() == null ? schedule.getStatus() : request.getStatus();
        if (requestedStatus == ScheduleStatus.CANCELLED && !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.SCHEDULE_CHANGE_REASON_REQUIRED);
        }
        if ((requestedStatus == ScheduleStatus.OPEN || requestedStatus == ScheduleStatus.CLOSED)
                && !departure.isAfter(today)) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_EDITABLE);
        }

        boolean wasFutureOpen = schedule.getStatus() == ScheduleStatus.OPEN
                && schedule.getDepartureDate().isAfter(today);
        boolean remainsFutureOpen = requestedStatus == ScheduleStatus.OPEN && departure.isAfter(today);
        readinessService.ensureCanRemoveFutureOpenSchedule(
                schedule.getTour(), scheduleId, wasFutureOpen && !remainsFutureOpen);

        schedule.setDepartureDate(departure);
        schedule.setReturnDate(returnDate);
        if (request.getPrice() != null) {
            schedule.setPrice(request.getPrice());
        }
        schedule.setStatus(requestedStatus);
        if (requestedStatus == ScheduleStatus.CANCELLED) {
            schedule.setCancellationReason(request.getReason().trim());
            schedule.setCancelledAt(LocalDateTime.now());
        }

        TourSchedule savedSchedule = tourScheduleRepository.save(schedule);

        if (requestedStatus == ScheduleStatus.CANCELLED) {
            notifyMatchingGroupMembersOfCancellation(savedSchedule);
        }

        return toResponse(savedSchedule);
    }

    private void notifyMatchingGroupMembersOfCancellation(TourSchedule schedule) {
        List<UUID> memberIds = matchingMemberRepository.findAcceptedMemberUserIdsByTourAndTargetDate(
                schedule.getTour().getTourId(), schedule.getDepartureDate(), JoinStatus.ACCEPTED);

        notificationService.notify(
                memberIds,
                NotificationEventType.SCHEDULE_UPDATED,
                ReferenceType.TOUR, schedule.getTour().getTourId(),
                "/trekker/my-groups",
                schedule.getTour().getTourName(), schedule.getDepartureDate().format(VN_DATE_FORMAT));
    }

    @Transactional
    public void deleteSchedule(String userEmail, UUID scheduleId) {
        TourSchedule schedule = tourScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        validateTourBelongsToVendor(schedule.getTour(), vendor);
        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_EDITABLE);
        }
        boolean removesFutureOpen = schedule.getStatus() == ScheduleStatus.OPEN
                && schedule.getDepartureDate().isAfter(LocalDate.now());
        readinessService.ensureCanRemoveFutureOpenSchedule(
                schedule.getTour(), scheduleId, removesFutureOpen);

        schedule.setIsDeleted(true);
        schedule.setDeletedAt(LocalDateTime.now());
        schedule.setDeletedBy(userEmail);
        tourScheduleRepository.save(schedule);
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void closePastOpenSchedules() {
        tourScheduleRepository.closePastOpenSchedules(LocalDate.now(), LocalDateTime.now());
    }

    private void validateTourBelongsToVendor(Tour tour, Vendor vendor) {
        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }
    }

    private void validateDatesAndDuration(Tour tour, LocalDate departure, LocalDate returnDate) {
        if (departure == null || !departure.isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.SCHEDULE_DEPARTURE_IN_PAST);
        }
        if (returnDate == null || returnDate.isBefore(departure)) {
            throw new AppException(ErrorCode.SCHEDULE_RETURN_BEFORE_DEPARTURE);
        }
        if (tour.getDurationDays() != null && tour.getDurationDays() > 0
                && returnDate.isAfter(departure.plusDays(tour.getDurationDays() - 1L))) {
            throw new AppException(
                    ErrorCode.SCHEDULE_DURATION_EXCEEDS_TOUR,
                    "Lịch khởi hành không được vượt quá thời lượng " + tour.getDurationDays() + " ngày của Tour.");
        }
    }

    private TourScheduleResponse toResponse(TourSchedule schedule) {
        return TourScheduleResponse.builder()
                .scheduleId(schedule.getTourScheduleId().toString())
                .tourId(schedule.getTour().getTourId().toString())
                .departureDate(schedule.getDepartureDate())
                .returnDate(schedule.getReturnDate())
                .price(schedule.getPrice())
                .status(schedule.getStatus())
                .cancellationReason(schedule.getCancellationReason())
                .cancelledAt(schedule.getCancelledAt())
                .isDeleted(schedule.getIsDeleted())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .createdBy(schedule.getCreatedBy())
                .updatedBy(schedule.getUpdatedBy())
                .deletedAt(schedule.getDeletedAt())
                .deletedBy(schedule.getDeletedBy())
                .build();
    }
}
