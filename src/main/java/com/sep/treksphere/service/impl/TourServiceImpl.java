package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.TourDetailResponse;
import com.sep.treksphere.dto.response.TourImageResponse;
import com.sep.treksphere.dto.response.TourScheduleResponse;
import com.sep.treksphere.dto.response.TourSummaryResponse;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourImage;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.enums.blog.ReviewStatus;
import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.enums.tour.TourStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.ReviewRepository;
import com.sep.treksphere.repository.TourImageRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.service.TourService;
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

        return toPaginationResponse(tourPage.map(this::toSummaryResponse));
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
                .tourId(tour.getTourID().toString())
                .tourName(tour.getTourName())
                .location(tour.getLocation())
                .durationDays(tour.getDurationDays())
                .basePrice(tour.getBasePrice())
                .difficulty(tour.getDifficulty())
                .status(tour.getStatus())
                .coverImageUrl(tour.getCoverImageUrl())
                .vendorId(tour.getVendor().getVendorID().toString())
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
                .tourId(tour.getTourID().toString())
                .tourName(tour.getTourName())
                .description(tour.getDescription())
                .difficulty(tour.getDifficulty())
                .location(tour.getLocation())
                .durationDays(tour.getDurationDays())
                .basePrice(tour.getBasePrice())
                .maxCapacity(tour.getMaxCapacity())
                .highlights(tour.getHighlights())
                .includes(tour.getIncludes())
                .excludes(tour.getExcludes())
                .coverImageUrl(tour.getCoverImageUrl())
                .status(tour.getStatus())
                .createdAt(tour.getCreatedAt())
                .updatedAt(tour.getUpdatedAt())
                // Vendor info
                .vendorId(tour.getVendor().getVendorID().toString())
                .vendorName(tour.getVendor().getCompanyName())
                .vendorLogoUrl(tour.getVendor().getLogoUrl())
                .vendorContactEmail(tour.getVendor().getContactEmail())
                .vendorContactPhone(tour.getVendor().getContactPhone())
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
                .imageId(image.getImageID().toString())
                .imageUrl(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .caption(image.getCaption())
                .build();
    }

    private TourScheduleResponse toScheduleResponse(TourSchedule schedule) {
        return TourScheduleResponse.builder()
                .scheduleId(schedule.getScheduleID().toString())
                .departureDate(schedule.getDepartureDate())
                .returnDate(schedule.getReturnDate())
                .availableSlots(schedule.getAvailableSlots())
                .bookedSlots(schedule.getBookedSlots())
                .price(schedule.getPrice())
                .status(schedule.getStatus())
                .build();
    }

    private <T> PaginationResponse<T> toPaginationResponse(Page<T> page) {
        return PaginationResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
