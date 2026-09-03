package com.sep.treksphere.tour.schedule;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourScheduleService {

    private final TourScheduleRepository tourScheduleRepository;
    private final TourRepository tourRepository;
    private final VendorRepository vendorRepository;

    @Transactional(readOnly = true)
    public List<TourScheduleResponse> getUpcomingSchedules(UUID tourId) {
        Tour tour = tourRepository.findById(tourId)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(
                        tour, ScheduleStatus.OPEN, LocalDate.now());

        return schedules.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TourScheduleResponse createSchedule(String userEmail, UUID tourId, CreateScheduleRequest request) {
        Tour tour = tourRepository.findById(tourId)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(tour, vendor);

        // Validation: departure date must be today or future
        if (request.getDepartureDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.SCHEDULE_DEPARTURE_IN_PAST);
        }

        // Validation: return date must be after or equal to departure date
        if (request.getReturnDate().isBefore(request.getDepartureDate())) {
            throw new AppException(ErrorCode.SCHEDULE_RETURN_BEFORE_DEPARTURE);
        }
        validateScheduleDuration(tour, request.getDepartureDate(), request.getReturnDate());

        TourSchedule schedule = new TourSchedule();
        schedule.setTour(tour);
        schedule.setDepartureDate(request.getDepartureDate());
        schedule.setReturnDate(request.getReturnDate());
        schedule.setPrice(request.getPrice());
        schedule.setStatus(ScheduleStatus.OPEN);

        TourSchedule savedSchedule = tourScheduleRepository.save(schedule);

        return toResponse(savedSchedule);
    }

    @Transactional
    public TourScheduleResponse updateSchedule(String userEmail, UUID scheduleId, UpdateScheduleRequest request) {
        TourSchedule schedule = tourScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(schedule.getTour(), vendor);

        // Validation: Lịch khởi hành đã CANCELLED thì không được phép sửa
        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_EDITABLE);
        }

        boolean isCancellingSchedule = request.getStatus() == ScheduleStatus.CANCELLED;
        if (isCancellingSchedule && !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.SCHEDULE_CHANGE_REASON_REQUIRED);
        }

        LocalDate departureDate = request.getDepartureDate() != null ? request.getDepartureDate()
                : schedule.getDepartureDate();
        LocalDate returnDate = request.getReturnDate() != null ? request.getReturnDate() : schedule.getReturnDate();

        if (request.getDepartureDate() != null) {
            if (departureDate.isBefore(LocalDate.now())) {
                throw new AppException(ErrorCode.SCHEDULE_DEPARTURE_IN_PAST);
            }
            schedule.setDepartureDate(departureDate);
        }

        if (request.getReturnDate() != null || request.getDepartureDate() != null) {
            if (returnDate.isBefore(departureDate)) {
                throw new AppException(ErrorCode.SCHEDULE_RETURN_BEFORE_DEPARTURE);
            }
            validateScheduleDuration(schedule.getTour(), departureDate, returnDate);
            schedule.setReturnDate(returnDate);
        }

        if (request.getPrice() != null) {
            schedule.setPrice(request.getPrice());
        }

        if (request.getStatus() != null) {
            schedule.setStatus(request.getStatus());
        }

        TourSchedule savedSchedule = tourScheduleRepository.save(schedule);

        return toResponse(savedSchedule);
    }

    @Transactional
    public void deleteSchedule(String userEmail, UUID scheduleId) {
        TourSchedule schedule = tourScheduleRepository.findByTourScheduleIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(schedule.getTour(), vendor);

        // Validation: Lịch khởi hành đã CANCELLED thì không được phép xóa
        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_EDITABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        schedule.setIsDeleted(true);
        schedule.setDeletedAt(now);
        schedule.setDeletedBy(userEmail);
        tourScheduleRepository.save(schedule);
    }

    // ======================== Helper Methods ========================

    /**
     * Chỉ còn Vendor Owner quản lý Tour — không còn VendorStaff.
     */
    private Vendor resolveVendorByUser(String email) {
        return vendorRepository.findByManager_Email(email)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));
    }

    private void validateTourBelongsToVendor(Tour tour, Vendor vendor) {
        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }
    }

    private void validateScheduleDuration(Tour tour, LocalDate departureDate, LocalDate returnDate) {
        Integer durationDays = tour.getDurationDays();
        if (durationDays != null && durationDays > 0
                && returnDate.isAfter(departureDate.plusDays(durationDays - 1L))) {
            throw new AppException(
                    ErrorCode.SCHEDULE_DURATION_EXCEEDS_TOUR,
                    "Lịch khởi hành không được vượt quá thời lượng " + durationDays + " ngày của Tour.");
        }
    }

    private TourScheduleResponse toResponse(TourSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return TourScheduleResponse.builder()
                .scheduleId(schedule.getTourScheduleId().toString())
                .tourId(schedule.getTour() != null ? schedule.getTour().getTourId().toString() : null)
                .departureDate(schedule.getDepartureDate())
                .returnDate(schedule.getReturnDate())
                .price(schedule.getPrice())
                .status(schedule.getStatus())
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
