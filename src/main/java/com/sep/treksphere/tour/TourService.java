package com.sep.treksphere.tour;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.util.PaginationUtils;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.notification.Notification;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationRepository;
import com.sep.treksphere.notification.ReferenceType;
import com.sep.treksphere.tour.checkpoint.TourCheckpoint;
import com.sep.treksphere.tour.checkpoint.TourCheckpointRepository;
import com.sep.treksphere.tour.checkpoint.TourCheckpointResponse;
import com.sep.treksphere.tour.dto.request.CreateTourRequest;
import com.sep.treksphere.tour.dto.request.UpdateTourRequest;
import com.sep.treksphere.tour.dto.response.PublicTourDetailResponse;
import com.sep.treksphere.tour.dto.response.TourDetailResponse;
import com.sep.treksphere.tour.dto.response.TourSummaryResponse;
import com.sep.treksphere.tour.image.TourImage;
import com.sep.treksphere.tour.image.TourImageRepository;
import com.sep.treksphere.tour.image.TourImageResponse;
import com.sep.treksphere.tour.schedule.ScheduleStatus;
import com.sep.treksphere.tour.schedule.TourSchedule;
import com.sep.treksphere.tour.schedule.TourScheduleRepository;
import com.sep.treksphere.tour.schedule.TourScheduleResponse;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourService {

    private static final Set<MatchingGroupStatus> ACTIVE_GROUP_STATUSES = EnumSet.of(
            MatchingGroupStatus.OPEN,
            MatchingGroupStatus.FULL,
            MatchingGroupStatus.CLOSED,
            MatchingGroupStatus.IN_PROGRESS);
    private static final List<TourStatus> VENDOR_VISIBLE_STATUSES = List.of(TourStatus.values());
    private static final Set<String> PUBLIC_SORT_FIELDS = Set.of(
            "createdAt", "publishedAt", "tourName", "difficulty", "durationDays");

    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;
    private final TourCheckpointRepository tourCheckpointRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final NotificationRepository notificationRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final UserRepository userRepository;
    private final TourMapper tourMapper;
    private final FileService fileService;
    private final VendorAccessService vendorAccessService;
    private final TourReadinessService readinessService;

    @Transactional(readOnly = true)
    public PaginationResponse<TourSummaryResponse> getTours(
            String keyword,
            String location,
            DifficultyLevel difficulty,
            LocalDate departureDate,
            LocalDate returnDate,
            UUID vendorId,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        String requestedSort = StringUtils.hasText(sortBy) ? sortBy.trim() : "publishedAt";
        String validSortBy = PUBLIC_SORT_FIELDS.contains(requestedSort) ? requestedSort : "publishedAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(validSortBy).ascending()
                : Sort.by(validSortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);

        Page<Tour> tourPage = tourRepository.searchTours(
                TourStatus.PUBLISHED,
                normalize(keyword),
                normalize(location),
                difficulty,
                departureDate,
                returnDate,
                vendorId,
                pageable);
        Map<UUID, BigDecimal> fromPrices = loadFromPrices(tourPage.getContent());
        return PaginationUtils.toPaginationResponse(
                tourPage.map(tour -> toSummaryResponse(tour, fromPrices.get(tour.getTourId()))));
    }

    @Transactional(readOnly = true)
    public PublicTourDetailResponse getTourById(UUID tourId) {
        Tour tour = tourRepository.findPublishedDetailById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        List<TourImage> images = activeImages(tour);
        List<TourCheckpoint> checkpoints = activeCheckpoints(tour);
        List<TourSchedule> schedules = upcomingSchedules(tour);
        return toPublicDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TourSummaryResponse> getVendorTours(String userEmail, BaseFilterRequest request) {
        Vendor vendor = vendorAccessService.resolveByManagerEmail(userEmail);
        Page<Tour> page = tourRepository.findByVendorIdForOwner(
                vendor.getVendorId(), VENDOR_VISIBLE_STATUSES, normalize(request.getKeyword()), request.getPageable());
        Map<UUID, BigDecimal> fromPrices = loadFromPrices(page.getContent());
        return PaginationUtils.toPaginationResponse(
                page.map(tour -> toSummaryResponse(tour, fromPrices.get(tour.getTourId()))));
    }

    @Transactional(readOnly = true)
    public TourDetailResponse getVendorTourById(String userEmail, UUID tourId) {
        Vendor vendor = vendorAccessService.resolveByManagerEmail(userEmail);
        return loadVendorDetail(getOwnedTour(tourId, vendor, false));
    }

    @Transactional
    public TourDetailResponse createTour(
            String userEmail,
            CreateTourRequest request,
            MultipartFile coverImage,
            List<MultipartFile> tourImages) {
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Tour tour = tourMapper.toTour(request);
        validateCapacity(tour);
        tour.setStatus(TourStatus.DRAFT);
        tour.setVendor(vendor);
        tour.setCreator(creator);
        if (coverImage != null && !coverImage.isEmpty()) {
            tour.setCoverImageUrl(fileService.uploadFile(coverImage, "tours"));
        }
        tour = tourRepository.save(tour);
        saveGallery(tour, tourImages);
        return loadVendorDetail(tour);
    }

    @Transactional
    public TourDetailResponse updateTour(
            String userEmail,
            UUID tourId,
            UpdateTourRequest request,
            MultipartFile coverImage,
            List<MultipartFile> tourImages) {
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        Tour tour = getOwnedTour(tourId, vendor, false);
        tourMapper.updateTourFromRequest(request, tour);
        validateCapacity(tour);
        validateExistingGroupCapacity(tour);
        if (coverImage != null && !coverImage.isEmpty()) {
            tour.setCoverImageUrl(fileService.uploadFile(coverImage, "tours"));
        }
        if (tour.getStatus() == TourStatus.PUBLISHED) {
            readinessService.validatePublishedStructure(tour);
        }
        tour = tourRepository.save(tour);
        if (tourImages != null) {
            List<TourImage> existing = activeImages(tour);
            if (!existing.isEmpty()) {
                tourImageRepository.deleteAll(existing);
            }
            saveGallery(tour, tourImages);
        }
        return loadVendorDetail(tour);
    }

    @Transactional
    public TourDetailResponse publishTour(String userEmail, UUID tourId) {
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        Tour tour = getOwnedTour(tourId, vendor, false);
        if (tour.getStatus() != TourStatus.DRAFT) {
            throw new AppException(ErrorCode.TOUR_NOT_DRAFT);
        }
        readinessService.validateForPublish(tour);
        tour.setStatus(TourStatus.PUBLISHED);
        if (tour.getPublishedAt() == null) {
            tour.setPublishedAt(LocalDateTime.now());
        }
        tourRepository.save(tour);
        return loadVendorDetail(tour);
    }

    @Transactional
    public TourDetailResponse unpublishTour(String userEmail, UUID tourId) {
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        Tour tour = getOwnedTour(tourId, vendor, false);
        if (tour.getStatus() != TourStatus.PUBLISHED) {
            throw new AppException(ErrorCode.TOUR_NOT_PUBLISHED);
        }
        ensureNoActiveGroups(tourId);
        tour.setStatus(TourStatus.DRAFT);
        tourRepository.save(tour);
        return loadVendorDetail(tour);
    }

    @Transactional
    public void deleteTour(String userEmail, UUID tourId) {
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        Tour tour = getOwnedTour(tourId, vendor, false);
        ensureNoActiveGroups(tourId);
        LocalDateTime now = LocalDateTime.now();
        tour.setIsDeleted(true);
        tour.setDeletedAt(now);
        tour.setDeletedBy(userEmail);
        tourRepository.save(tour);
        tourCheckpointRepository.softDeleteByTourId(tourId, now, userEmail);
        tourScheduleRepository.softDeleteByTourId(tourId, now, userEmail);
        tourImageRepository.softDeleteByTourId(tourId, now, userEmail);
    }

    @Transactional
    public TourDetailResponse restoreTour(String userEmail, UUID tourId) {
        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userEmail);
        Tour tour = getOwnedTour(tourId, vendor, true);
        LocalDateTime deletedAt = tour.getDeletedAt();
        tour.setIsDeleted(false);
        tour.setDeletedAt(null);
        tour.setDeletedBy(null);
        tour.setStatus(TourStatus.DRAFT);
        tourRepository.save(tour);
        tourCheckpointRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);
        tourScheduleRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);
        tourImageRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);
        return loadVendorDetail(tour);
    }

    @Transactional
    public TourDetailResponse hideTourForViolation(UUID adminId, UUID tourId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, MessageConstant.HIDE_REASON_REQUIRED);
        }
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        if (tour.getStatus() != TourStatus.PUBLISHED) {
            throw new AppException(ErrorCode.TOUR_NOT_PUBLISHED);
        }
        tour.setStatus(TourStatus.HIDDEN);
        tour.setHiddenReason(reason.trim());
        tour.setHiddenAt(LocalDateTime.now());
        tour.setHiddenBy(admin);
        tourRepository.save(tour);
        notifyTourHidden(tour, reason.trim());
        return loadVendorDetail(tour);
    }

    @Transactional
    public TourDetailResponse unhideTour(UUID adminId, UUID tourId) {
        userRepository.findById(adminId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        if (tour.getStatus() != TourStatus.HIDDEN) {
            throw new AppException(ErrorCode.TOUR_NOT_HIDDEN);
        }
        vendorAccessService.requireActive(tour.getVendor());
        readinessService.validateForPublish(tour);
        tour.setStatus(TourStatus.PUBLISHED);
        if (tour.getPublishedAt() == null) {
            tour.setPublishedAt(LocalDateTime.now());
        }
        tourRepository.save(tour);
        return loadVendorDetail(tour);
    }

    public TourSummaryResponse toSummaryResponse(Tour tour, BigDecimal fromPrice) {
        return TourSummaryResponse.builder()
                .tourId(tour.getTourId().toString())
                .tourName(tour.getTourName())
                .location(tour.getLocation())
                .durationDays(tour.getDurationDays())
                .fromPrice(fromPrice)
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
                .createdAt(tour.getCreatedAt())
                .publishedAt(tour.getPublishedAt())
                .build();
    }

    public Map<UUID, BigDecimal> loadFromPrices(List<Tour> tours) {
        if (tours.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = tourScheduleRepository.findMinOpenPriceByTourIds(
                tours.stream().map(Tour::getTourId).toList(), LocalDate.now());
        return rows.stream().collect(Collectors.toMap(
                row -> (UUID) row[0], row -> (BigDecimal) row[1]));
    }

    private Tour getOwnedTour(UUID tourId, Vendor vendor, boolean deleted) {
        Tour tour = deleted
                ? tourRepository.findByTourIdAndIsDeletedTrue(tourId)
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_DELETED))
                : tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }
        return tour;
    }

    private void ensureNoActiveGroups(UUID tourId) {
        if (matchingGroupRepository.existsByTour_TourIdAndStatusInAndIsDeletedFalse(
                tourId, ACTIVE_GROUP_STATUSES)) {
            throw new AppException(ErrorCode.TOUR_HAS_ACTIVE_GROUPS);
        }
    }

    private void validateCapacity(Tour tour) {
        if (tour.getMinCapacity() == null || tour.getMaxCapacity() == null
                || tour.getMinCapacity() < 1 || tour.getMaxCapacity() < tour.getMinCapacity()) {
            throw new AppException(ErrorCode.INVALID_TOUR_CAPACITY);
        }
    }

    private void validateExistingGroupCapacity(Tour tour) {
        UUID tourId = tour.getTourId();
        if (tourId != null && (matchingGroupRepository
                .existsByTour_TourIdAndMaxSizeGreaterThanAndStatusInAndIsDeletedFalse(
                        tourId, tour.getMaxCapacity(), ACTIVE_GROUP_STATUSES)
                || matchingGroupRepository
                .existsByTour_TourIdAndMaxSizeLessThanAndStatusInAndIsDeletedFalse(
                        tourId, tour.getMinCapacity(), ACTIVE_GROUP_STATUSES))) {
            throw new AppException(
                    ErrorCode.INVALID_TOUR_CAPACITY,
                    "Sức chứa mới không tương thích với nhóm ghép đã tồn tại.");
        }
    }

    private TourDetailResponse loadVendorDetail(Tour tour) {
        List<TourImage> images = activeImages(tour);
        List<TourCheckpoint> checkpoints = activeCheckpoints(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        return toVendorDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    private List<TourImage> activeImages(Tour tour) {
        return tourImageRepository.findByTourAndIsDeletedFalseOrderBySortOrderAsc(tour);
    }

    private List<TourCheckpoint> activeCheckpoints(Tour tour) {
        return tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
    }

    private List<TourSchedule> upcomingSchedules(Tour tour) {
        return tourScheduleRepository
                .findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(
                        tour, ScheduleStatus.OPEN, LocalDate.now());
    }

    private void saveGallery(Tour tour, List<MultipartFile> tourImages) {
        if (tourImages == null || tourImages.isEmpty()) {
            return;
        }
        List<String> urls = fileService.uploadFiles(tourImages, "tours");
        List<TourImage> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            TourImage image = new TourImage();
            image.setTour(tour);
            image.setImageUrl(urls.get(i));
            image.setSortOrder(i);
            images.add(image);
        }
        tourImageRepository.saveAll(images);
    }

    private void notifyTourHidden(Tour tour, String reason) {
        Notification notification = new Notification();
        notification.setRecipient(tour.getVendor().getManager());
        notification.setTitle("Tour bị ẩn do vi phạm");
        notification.setEventType(NotificationEventType.TOUR_HIDDEN_VIOLATION);
        notification.setContent("Tour \"" + tour.getTourName() + "\" đã bị ẩn. Lý do: " + reason);
        notification.setReferenceType(ReferenceType.TOUR);
        notification.setReferenceId(tour.getTourId());
        notificationRepository.save(notification);
    }

    private BigDecimal minPriceOf(List<TourSchedule> schedules) {
        return schedules.stream()
                .filter(schedule -> schedule.getStatus() == ScheduleStatus.OPEN)
                .map(TourSchedule::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private TourDetailResponse toVendorDetailResponse(
            Tour tour,
            List<TourImage> images,
            List<TourCheckpoint> checkpoints,
            List<TourSchedule> schedules,
            BigDecimal fromPrice) {
        List<String> readinessErrors = readinessService.getPublishReadinessErrors(tour);
        return TourDetailResponse.builder()
                .tourId(tour.getTourId().toString())
                .tourName(tour.getTourName())
                .description(tour.getDescription())
                .difficulty(tour.getDifficulty())
                .location(tour.getLocation())
                .durationDays(tour.getDurationDays())
                .fromPrice(fromPrice)
                .minCapacity(tour.getMinCapacity())
                .maxCapacity(tour.getMaxCapacity())
                .totalDistanceKm(tour.getTotalDistanceKm())
                .highlights(tour.getHighlights())
                .includes(tour.getIncludes())
                .excludes(tour.getExcludes())
                .coverImageUrl(tour.getCoverImageUrl())
                .status(tour.getStatus())
                .hiddenReason(tour.getHiddenReason())
                .publishedAt(tour.getPublishedAt())
                .hiddenAt(tour.getHiddenAt())
                .hiddenBy(tour.getHiddenBy() == null ? null : tour.getHiddenBy().getUserId().toString())
                .createdAt(tour.getCreatedAt())
                .updatedAt(tour.getUpdatedAt())
                .vendorId(tour.getVendor().getVendorId().toString())
                .vendorManagerId(tour.getVendor().getManager().getUserId().toString())
                .vendorName(tour.getVendor().getCompanyName())
                .vendorLogoUrl(tour.getVendor().getLogoUrl())
                .vendorContactEmail(tour.getVendor().getContactEmail())
                .vendorContactPhone(tour.getVendor().getContactPhone())
                .creatorId(tour.getCreator().getUserId().toString())
                .creatorName(tour.getCreator().getFullName())
                .creatorEmail(tour.getCreator().getEmail())
                .images(images.stream().map(this::toImageResponse).toList())
                .checkpoints(checkpoints.stream().map(this::toCheckpointResponse).toList())
                .schedules(schedules.stream().map(this::toScheduleResponse).toList())
                .publishable(readinessErrors.isEmpty())
                .publishReadinessErrors(readinessErrors)
                .build();
    }

    private PublicTourDetailResponse toPublicDetailResponse(
            Tour tour,
            List<TourImage> images,
            List<TourCheckpoint> checkpoints,
            List<TourSchedule> schedules,
            BigDecimal fromPrice) {
        return PublicTourDetailResponse.builder()
                .tourId(tour.getTourId().toString())
                .tourName(tour.getTourName())
                .description(tour.getDescription())
                .difficulty(tour.getDifficulty())
                .location(tour.getLocation())
                .durationDays(tour.getDurationDays())
                .fromPrice(fromPrice)
                .minCapacity(tour.getMinCapacity())
                .maxCapacity(tour.getMaxCapacity())
                .totalDistanceKm(tour.getTotalDistanceKm())
                .highlights(tour.getHighlights())
                .includes(tour.getIncludes())
                .excludes(tour.getExcludes())
                .coverImageUrl(tour.getCoverImageUrl())
                .publishedAt(tour.getPublishedAt())
                .vendorId(tour.getVendor().getVendorId().toString())
                .vendorName(tour.getVendor().getCompanyName())
                .vendorLogoUrl(tour.getVendor().getLogoUrl())
                .vendorContactEmail(tour.getVendor().getContactEmail())
                .vendorContactPhone(tour.getVendor().getContactPhone())
                .images(images.stream().map(this::toImageResponse).toList())
                .checkpoints(checkpoints.stream().map(this::toCheckpointResponse).toList())
                .schedules(schedules.stream().map(this::toScheduleResponse).toList())
                .build();
    }

    private TourCheckpointResponse toCheckpointResponse(TourCheckpoint checkpoint) {
        String rawUrls = checkpoint.getCheckpointImageUrl();
        List<String> urls = rawUrls == null || rawUrls.isBlank() ? List.of() : List.of(rawUrls.split(","));
        return TourCheckpointResponse.builder()
                .checkpointId(checkpoint.getTourCheckpointId().toString())
                .tourId(checkpoint.getTour().getTourId().toString())
                .checkpointName(checkpoint.getCheckpointName())
                .description(checkpoint.getDescription())
                .latitude(checkpoint.getLatitude())
                .longitude(checkpoint.getLongitude())
                .altitude(checkpoint.getAltitude())
                .checkpointOrder(checkpoint.getCheckpointOrder())
                .checkpointImageUrl(rawUrls)
                .checkpointImageUrls(urls)
                .build();
    }

    private TourImageResponse toImageResponse(TourImage image) {
        return TourImageResponse.builder()
                .imageId(image.getTourImageId().toString())
                .imageUrl(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .caption(image.getCaption())
                .build();
    }

    private TourScheduleResponse toScheduleResponse(TourSchedule schedule) {
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
                .build();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
