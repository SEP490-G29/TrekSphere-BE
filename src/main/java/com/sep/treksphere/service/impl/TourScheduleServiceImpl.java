package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.CreateScheduleRequest;
import com.sep.treksphere.dto.request.UpdateScheduleRequest;
import com.sep.treksphere.dto.response.TourScheduleResponse;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.TourScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourScheduleServiceImpl implements TourScheduleService {

    private final TourScheduleRepository tourScheduleRepository;
    private final TourRepository tourRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final BookingRepository bookingRepository;

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

        TourSchedule schedule = new TourSchedule();
        schedule.setTour(tour);
        schedule.setDepartureDate(request.getDepartureDate());
        schedule.setReturnDate(request.getReturnDate());
        schedule.setPrice(request.getPrice());
        schedule.setAvailableSlots(request.getAvailableSlots());
        schedule.setBookedSlots(0);
        schedule.setStatus(ScheduleStatus.OPEN);

        return toResponse(tourScheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public TourScheduleResponse updateSchedule(String userEmail, UUID scheduleId, UpdateScheduleRequest request) {
        TourSchedule schedule = tourScheduleRepository.findByScheduleIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(schedule.getTour(), vendor);

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
            schedule.setAvailableSlots(request.getAvailableSlots());
        }

        if (request.getStatus() != null) {
            schedule.setStatus(request.getStatus());
        }

        return toResponse(tourScheduleRepository.save(schedule));
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

        // Check if there are active bookings
        boolean hasBookings = bookingRepository.existsByScheduleAndBookingStatusNotAndIsDeletedFalse(schedule, BookingStatus.CANCELLED)
                || (schedule.getBookedSlots() != null && schedule.getBookedSlots() > 0);
        if (hasBookings) {
            throw new AppException(ErrorCode.SCHEDULE_HAS_BOOKINGS);
        }

        schedule.setIsDeleted(true);
        schedule.setDeletedAt(LocalDateTime.now());
        schedule.setDeletedBy(userEmail);
        tourScheduleRepository.save(schedule);
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
                .departureDate(schedule.getDepartureDate())
                .returnDate(schedule.getReturnDate())
                .availableSlots(schedule.getAvailableSlots())
                .bookedSlots(schedule.getBookedSlots())
                .price(schedule.getPrice())
                .status(schedule.getStatus())
                .build();
    }
}
