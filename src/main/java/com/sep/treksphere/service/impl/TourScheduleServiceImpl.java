package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.CreateScheduleRequest;
import com.sep.treksphere.dto.request.UpdateScheduleRequest;
import com.sep.treksphere.dto.response.TourScheduleResponse;
import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.Notification;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import com.sep.treksphere.enums.tour.TourStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.TourSessionRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.TourScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourScheduleServiceImpl implements TourScheduleService {

    private final TourScheduleRepository tourScheduleRepository;
    private final TourSessionRepository tourSessionRepository;
    private final TourRepository tourRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;

    @Override
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

    @Override
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

        // Validation: availableSlots không được vượt quá maxCapacity của Tour
        if (tour.getMaxCapacity() != null && request.getAvailableSlots() > tour.getMaxCapacity()) {
            throw new AppException(ErrorCode.SCHEDULE_SLOTS_EXCEED_MAX_CAPACITY);
        }

        TourSchedule schedule = new TourSchedule();
        schedule.setTour(tour);
        schedule.setDepartureDate(request.getDepartureDate());
        schedule.setReturnDate(request.getReturnDate());
        schedule.setPrice(request.getPrice());
        schedule.setAvailableSlots(request.getAvailableSlots());
        schedule.setBookedSlots(0);

        // Nếu Tour chưa APPROVED/HIDDEN, Schedule sẽ khởi tạo với trạng thái CLOSED (chưa mở đặt chỗ)
        ScheduleStatus initialStatus = (tour.getStatus() == TourStatus.APPROVED || tour.getStatus() == TourStatus.HIDDEN)
                ? ScheduleStatus.OPEN
                : ScheduleStatus.CLOSED;
        schedule.setStatus(initialStatus);

        TourSchedule savedSchedule = tourScheduleRepository.save(schedule);

        TourSession session = new TourSession();
        session.setTourSchedule(savedSchedule);
        session.setStatus(TourSessionStatus.PENDING);
        tourSessionRepository.save(session);

        return toResponse(savedSchedule);
    }

    @Override
    @Transactional
    public TourScheduleResponse updateSchedule(String userEmail, UUID scheduleId, UpdateScheduleRequest request) {
        TourSchedule schedule = tourScheduleRepository.findByScheduleIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(schedule.getTour(), vendor);

        // Validation: Lịch khởi hành đã COMPLETED hoặc CANCELLED thì không được phép sửa
        if (schedule.getStatus() == ScheduleStatus.COMPLETED || schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_EDITABLE);
        }

        // Kiểm tra xem Schedule đã có khách đặt chỗ chưa
        boolean hasBookings = bookingRepository.existsByScheduleAndBookingStatusNotAndIsDeletedFalse(schedule, BookingStatus.CANCELLED)
                || (schedule.getBookedSlots() != null && schedule.getBookedSlots() > 0);

        // Nếu ĐÃ CÓ khách đặt, yêu cầu bắt buộc phải truyền lý do (reason)
        if (hasBookings && !StringUtils.hasText(request.getReason())) {
            throw new AppException(ErrorCode.SCHEDULE_CHANGE_REASON_REQUIRED);
        }

        LocalDate departureDate = request.getDepartureDate() != null ? request.getDepartureDate() : schedule.getDepartureDate();
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
            schedule.setReturnDate(returnDate);
        }

        if (request.getPrice() != null) {
            schedule.setPrice(request.getPrice());
        }

        if (request.getAvailableSlots() != null) {
            int currentBooked = schedule.getBookedSlots() != null ? schedule.getBookedSlots() : 0;
            if (request.getAvailableSlots() < currentBooked) {
                throw new AppException(ErrorCode.SCHEDULE_SLOTS_LESS_THAN_BOOKED);
            }
            if (schedule.getTour().getMaxCapacity() != null && request.getAvailableSlots() > schedule.getTour().getMaxCapacity()) {
                throw new AppException(ErrorCode.SCHEDULE_SLOTS_EXCEED_MAX_CAPACITY);
            }
            schedule.setAvailableSlots(request.getAvailableSlots());
        }

        if (request.getStatus() != null) {
            schedule.setStatus(request.getStatus());
        }

        TourSchedule savedSchedule = tourScheduleRepository.save(schedule);

        // Nếu ĐÃ CÓ khách đặt, gửi Notification tới từng khách hàng có Booking active
        if (hasBookings) {
            List<Booking> activeBookings = bookingRepository.findByScheduleAndBookingStatusNotAndIsDeletedFalse(schedule, BookingStatus.CANCELLED);
            for (Booking booking : activeBookings) {
                Notification notification = new Notification();
                notification.setRecipient(booking.getUser());
                notification.setTitle("Lịch khởi hành tour đã có sự thay đổi");
                notification.setEventType(NotificationEventType.SCHEDULE_UPDATED);
                notification.setContent("Lịch khởi hành tour \"" + schedule.getTour().getTourName() +
                        "\" (khởi hành ngày " + savedSchedule.getDepartureDate() + ") đã được điều chỉnh. Lý do: " + request.getReason().trim());
                notification.setReferenceType(ReferenceType.BOOKING);
                notification.setReferenceId(booking.getBookingId());
                notificationRepository.save(notification);
            }
        }

        return toResponse(savedSchedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(String userEmail, UUID scheduleId) {
        TourSchedule schedule = tourScheduleRepository.findByScheduleIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        // Only VendorManager can delete
        Vendor vendor = vendorRepository.findByManager_Email(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));
        validateTourBelongsToVendor(schedule.getTour(), vendor);

        // Validation: Lịch khởi hành đã COMPLETED hoặc CANCELLED thì không được phép xóa
        if (schedule.getStatus() == ScheduleStatus.COMPLETED || schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_EDITABLE);
        }

        // Check if there are active bookings
        boolean hasBookings = bookingRepository.existsByScheduleAndBookingStatusNotAndIsDeletedFalse(schedule, BookingStatus.CANCELLED)
                || (schedule.getBookedSlots() != null && schedule.getBookedSlots() > 0);
        if (hasBookings) {
            throw new AppException(ErrorCode.SCHEDULE_HAS_BOOKINGS);
        }

        LocalDateTime now = LocalDateTime.now();
        schedule.setIsDeleted(true);
        schedule.setDeletedAt(now);
        schedule.setDeletedBy(userEmail);
        tourScheduleRepository.save(schedule);

        // Đồng bộ xóa mềm TourSession liên kết với schedule này
        tourSessionRepository.findByTourSchedule_ScheduleIdAndIsDeletedFalse(scheduleId)
                .ifPresent(session -> {
                    session.setIsDeleted(true);
                    session.setDeletedAt(now);
                    session.setDeletedBy(userEmail);
                    tourSessionRepository.save(session);
                });
    }

    // ======================== Helper Methods ========================

    private Vendor resolveVendorByUser(String email) {
        Optional<Vendor> vendorOpt = vendorRepository.findByManager_Email(email);
        if (vendorOpt.isPresent()) {
            return vendorOpt.get();
        }

        return vendorStaffRepository.findByUser_Email(email)
                .map(VendorStaff::getVendor)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));
    }

    private void validateTourBelongsToVendor(Tour tour, Vendor vendor) {
        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }
    }

    private TourScheduleResponse toResponse(TourSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return TourScheduleResponse.builder()
                .scheduleId(schedule.getScheduleId().toString())
                .tourId(schedule.getTour() != null ? schedule.getTour().getTourId().toString() : null)
                .departureDate(schedule.getDepartureDate())
                .returnDate(schedule.getReturnDate())
                .availableSlots(schedule.getAvailableSlots())
                .bookedSlots(schedule.getBookedSlots())
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
