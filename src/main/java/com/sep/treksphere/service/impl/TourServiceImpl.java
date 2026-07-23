package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.CreateTourRequest;
import com.sep.treksphere.dto.request.UpdateTourRequest;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.entity.Notification;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourImage;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.blog.ReviewStatus;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.enums.tour.TourStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.TourMapper;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.repository.ReviewRepository;
import com.sep.treksphere.repository.TourImageRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.TourService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final UserRepository userRepository;
    private final TourMapper tourMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<TourSummaryResponse> getTours(
            String keyword,
            String location,
            DifficultyLevel difficulty,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedLocation = StringUtils.hasText(location) ? location.trim() : null;

        Page<Tour> tourPage = tourRepository.searchTours(
                TourStatus.APPROVED,
                normalizedKeyword,
                normalizedLocation,
                difficulty,
                pageable);

        return PaginationUtils.toPaginationResponse(tourPage.map(this::toSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse getTourById(UUID tourId) {
        Tour tour = tourRepository.findDetailById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndStatusOrderByDepartureDateAsc(tour, ScheduleStatus.OPEN);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);
        return toDetailResponse(tour, images, schedules, avgRating, totalReviews);
    }

    private TourSummaryResponse toSummaryResponse(Tour tour) {
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return TourSummaryResponse.builder()
                .tourId(tour.getTourId().toString())
                .tourName(tour.getTourName())
                .location(tour.getLocation())
                .durationDays(tour.getDurationDays())
                .basePrice(tour.getBasePrice())
                .minCapacity(tour.getMinCapacity())
                .maxCapacity(tour.getMaxCapacity())
                .totalDistanceKm(tour.getTotalDistanceKm())
                .difficulty(tour.getDifficulty())
                .status(tour.getStatus())
                .coverImageUrl(tour.getCoverImageUrl())
                .highlights(tour.getHighlights())
                .includes(tour.getIncludes())
                .excludes(tour.getExcludes())
                .vendorId(tour.getVendor().getVendorId().toString())
                .vendorName(tour.getVendor().getCompanyName())
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .createdAt(tour.getCreatedAt())
                .build();
    }

    private TourDetailResponse toDetailResponse(
            Tour tour,
            List<TourImage> images,
            List<TourSchedule> schedules,
            Double avgRating,
            int totalReviews) {
        return TourDetailResponse.builder()
                // Tour info
                .tourId(tour.getTourId().toString())
                .tourName(tour.getTourName())
                .description(tour.getDescription())
                .difficulty(tour.getDifficulty())
                .location(tour.getLocation())
                .durationDays(tour.getDurationDays())
                .basePrice(tour.getBasePrice())
                .minCapacity(tour.getMinCapacity())
                .maxCapacity(tour.getMaxCapacity())
                .totalDistanceKm(tour.getTotalDistanceKm())
                .highlights(tour.getHighlights())
                .includes(tour.getIncludes())
                .excludes(tour.getExcludes())
                .coverImageUrl(tour.getCoverImageUrl())
                .status(tour.getStatus())
                .createdAt(tour.getCreatedAt())
                .updatedAt(tour.getUpdatedAt())
                // Vendor info
                .vendorId(tour.getVendor().getVendorId().toString())
                .vendorName(tour.getVendor().getCompanyName())
                .vendorLogoUrl(tour.getVendor().getLogoUrl())
                .vendorContactEmail(tour.getVendor().getContactEmail())
                .vendorContactPhone(tour.getVendor().getContactPhone())
                // Creator info
                .creatorId(tour.getCreator() != null ? tour.getCreator().getUserId().toString() : null)
                .creatorName(tour.getCreator() != null ? tour.getCreator().getFullName() : null)
                .creatorEmail(tour.getCreator() != null ? tour.getCreator().getEmail() : null)
                // Images
                .images(images.stream().map(this::toImageResponse).toList())
                // Schedules
                .schedules(schedules.stream().map(this::toScheduleResponse).toList())
                // Review stats
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .build();
    }

    private TourImageResponse toImageResponse(TourImage image) {
        return TourImageResponse.builder()
                .imageId(image.getImageId().toString())
                .imageUrl(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .caption(image.getCaption())
                .build();
    }

    private TourScheduleResponse toScheduleResponse(TourSchedule schedule) {
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

    // --- Vendor Tour Management Methods ---

    private Vendor resolveVendorByEmail(String email) {
        return vendorRepository.findByManager_Email(email)
                .orElseGet(() -> vendorStaffRepository.findByUser_Email(email)
                        .orElseThrow(() -> new AppException(ErrorCode.VENDOR_STAFF_NOT_FOUND))
                        .getVendor());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<TourSummaryResponse> getVendorTours(String userEmail, BaseFilterRequest request) {
        Vendor vendor = resolveVendorByEmail(userEmail);
        
        Page<Tour> tourPage = tourRepository.findByVendorIdAndKeyword(
                vendor.getVendorId(),
                request.getKeyword(),
                request.getPageable()
        );

        return PaginationUtils.toPaginationResponse(tourPage.map(this::toSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse getVendorTourById(String userEmail, UUID tourId) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndStatusOrderByDepartureDateAsc(tour, ScheduleStatus.OPEN);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, schedules, avgRating, totalReviews);
    }

    @Override
    @Transactional
    public TourDetailResponse createTour(String userEmail, CreateTourRequest request) {
        Vendor vendor = resolveVendorByEmail(userEmail);
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Tour tour = tourMapper.toTour(request);
        tour.setStatus(TourStatus.DRAFT);
        tour.setVendor(vendor);
        tour.setCreator(creator);

        tour = tourRepository.save(tour);
        
        return toDetailResponse(tour, List.of(), List.of(), 0.0, 0);
    }

    @Override
    @Transactional
    public TourDetailResponse updateTour(String userEmail, UUID tourId, UpdateTourRequest request) {
        Vendor vendor = resolveVendorByEmail(userEmail);
        
        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
                
        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.REJECTED) {
            throw new AppException(ErrorCode.TOUR_STATUS_NOT_EDITABLE);
        }

        tourMapper.updateTourFromRequest(request, tour);
        tour = tourRepository.save(tour);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndStatusOrderByDepartureDateAsc(tour, ScheduleStatus.OPEN);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);
        
        return toDetailResponse(tour, images, schedules, avgRating, totalReviews);
    }

    @Override
    @Transactional
    public void deleteTour(String userEmail, UUID tourId) {
        Vendor vendor = vendorRepository.findByManager_Email(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
                
        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        tour.setIsDeleted(true);
        tourRepository.save(tour);
    }

    // --- Tour Approval Workflow ---

    @Override
    @Transactional
    public TourDetailResponse submitTourForApproval(String userEmail, UUID tourId) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.REJECTED) {
            throw new AppException(ErrorCode.TOUR_NOT_IN_DRAFT_OR_REJECTED);
        }

        tour.setStatus(TourStatus.PENDING_APPROVAL);
        tour = tourRepository.save(tour);

        // Send notification to Vendor Manager
        User manager = vendor.getManager();
        Notification notification = new Notification();
        notification.setRecipient(manager);
        notification.setTitle("Yêu cầu duyệt Tour mới");
        notification.setEventType(NotificationEventType.TOUR_PENDING_APPROVAL);
        notification.setContent("Tour \"" + tour.getTourName() + "\" đã được gửi yêu cầu kiểm duyệt.");
        notification.setReferenceType(ReferenceType.TOUR);
        notification.setReferenceId(tour.getTourId());
        notificationRepository.save(notification);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndStatusOrderByDepartureDateAsc(tour, ScheduleStatus.OPEN);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, schedules, avgRating, totalReviews);
  }

    @Override
    @Transactional
    public TourDetailResponse hideTourForViolation(String userEmail, UUID tourId, String reason) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getRoleName()));

        if (!isAdmin) {
            Vendor vendor = resolveVendorByEmail(userEmail);
            if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
                throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
            }
        }
        if (tour.getStatus() != TourStatus.APPROVED) {
            throw new AppException(ErrorCode.TOUR_NOT_APPROVED);
        }
        tour.setStatus(TourStatus.HIDDEN);
        tour = tourRepository.save(tour);

        User manager = tour.getVendor().getManager();
        Notification notification = new Notification();
        notification.setRecipient(manager);
        notification.setTitle("Tour bị ẩn do vi phạm");
        notification.setEventType(NotificationEventType.TOUR_HIDDEN_VIOLATION);
        notification.setContent("Tour \"" + tour.getTourName() + "\" đã bị ẩn. Lý do: " + reason);
        notification.setReferenceType(ReferenceType.TOUR);
        notification.setReferenceId(tour.getTourId());
        notificationRepository.save(notification);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndStatusOrderByDepartureDateAsc(tour, ScheduleStatus.OPEN);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, schedules, avgRating, totalReviews);
    }
}
