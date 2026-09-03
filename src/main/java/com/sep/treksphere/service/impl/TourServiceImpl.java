package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.CreateTourRequest;
import com.sep.treksphere.dto.request.UpdateTourRequest;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.entity.Notification;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourCheckpoint;
import com.sep.treksphere.entity.TourImage;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.enums.tour.TourStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.TourMapper;
import com.sep.treksphere.repository.MatchingGroupRepository;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.repository.TourCheckpointRepository;
import com.sep.treksphere.repository.TourImageRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.service.FileService;
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
public class TourServiceImpl implements TourService {

    private static final Set<MatchingGroupStatus> ACTIVE_GROUP_STATUSES = EnumSet.of(
            MatchingGroupStatus.OPEN,
            MatchingGroupStatus.FULL,
            MatchingGroupStatus.IN_PROGRESS);

    private static final List<TourStatus> VENDOR_VISIBLE_STATUSES = List.of(
            TourStatus.DRAFT,
            TourStatus.PENDING_APPROVAL,
            TourStatus.APPROVED,
            TourStatus.HIDDEN,
            TourStatus.REJECTED);

    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;
    private final TourCheckpointRepository tourCheckpointRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final NotificationRepository notificationRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final TourMapper tourMapper;
    private final FileService fileService;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<TourSummaryResponse> getTours(
            String keyword,
            String location,
            DifficultyLevel difficulty,
            LocalDate departureDate,
            LocalDate returnDate,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        String validSortBy = StringUtils.hasText(sortBy) ? sortBy.trim() : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(validSortBy).ascending()
                : Sort.by(validSortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedLocation = StringUtils.hasText(location) ? location.trim() : null;

        Page<Tour> tourPage = tourRepository.searchTours(
                TourStatus.APPROVED,
                normalizedKeyword,
                normalizedLocation,
                difficulty,
                departureDate,
                returnDate,
                pageable);

        Map<UUID, BigDecimal> fromPriceByTourId = loadFromPrices(tourPage.getContent());
        return PaginationUtils.toPaginationResponse(
                tourPage.map(tour -> toSummaryResponse(tour, fromPriceByTourId.get(tour.getTourId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse getTourById(UUID tourId) {
        Tour tour = tourRepository.findDetailById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(
                        tour, ScheduleStatus.OPEN, LocalDate.now());
        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    private Map<UUID, BigDecimal> loadFromPrices(List<Tour> tours) {
        if (tours.isEmpty()) {
            return Map.of();
        }
        List<UUID> tourIds = tours.stream().map(Tour::getTourId).toList();
        List<Object[]> rows = tourScheduleRepository.findMinOpenPriceByTourIds(tourIds, LocalDate.now());
        return rows.stream().collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (BigDecimal) row[1]));
    }

    private BigDecimal minPriceOf(List<TourSchedule> schedules) {
        return schedules.stream()
                .map(TourSchedule::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private TourSummaryResponse toSummaryResponse(Tour tour, BigDecimal fromPrice) {
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
                .build();
    }

    private TourDetailResponse toDetailResponse(
            Tour tour,
            List<TourImage> images,
            List<TourCheckpoint> checkpoints,
            List<TourSchedule> schedules,
            BigDecimal fromPrice) {
        return TourDetailResponse.builder()
                // Tour info
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
                .rejectionReason(tour.getRejectionReason())
                .createdAt(tour.getCreatedAt())
                .updatedAt(tour.getUpdatedAt())
                // Vendor info
                .vendorId(tour.getVendor().getVendorId().toString())
                .vendorManagerId(tour.getVendor().getManager().getUserId().toString())
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
                // Checkpoints
                .checkpoints(checkpoints.stream().map(this::toCheckpointResponse).toList())
                // Schedules
                .schedules(schedules.stream().map(this::toScheduleResponse).toList())
                .build();
    }

    private TourCheckpointResponse toCheckpointResponse(TourCheckpoint checkpoint) {
        String rawUrls = checkpoint.getCheckpointImageUrl();
        List<String> imageUrlList = (rawUrls != null && !rawUrls.isBlank())
                ? List.of(rawUrls.split(","))
                : List.of();

        return TourCheckpointResponse.builder()
                .checkpointId(checkpoint.getTourCheckpointId().toString())
                .tourId(checkpoint.getTour() != null ? checkpoint.getTour().getTourId().toString() : null)
                .checkpointName(checkpoint.getCheckpointName())
                .description(checkpoint.getDescription())
                .latitude(checkpoint.getLatitude())
                .longitude(checkpoint.getLongitude())
                .altitude(checkpoint.getAltitude())
                .checkpointOrder(checkpoint.getCheckpointOrder())
                .checkpointImageUrl(rawUrls)
                .checkpointImageUrls(imageUrlList)
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
                .tourId(schedule.getTour() != null ? schedule.getTour().getTourId().toString() : null)
                .departureDate(schedule.getDepartureDate())
                .returnDate(schedule.getReturnDate())
                .price(schedule.getPrice())
                .status(schedule.getStatus())
                .isDeleted(schedule.getIsDeleted())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }

    // --- Vendor Tour Management Methods ---

    private Vendor resolveVendorByEmail(String email) {
        return vendorRepository.findByManager_Email(email)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<TourSummaryResponse> getVendorTours(String userEmail, BaseFilterRequest request) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Page<Tour> tourPage = tourRepository.findByVendorIdForOwner(
                vendor.getVendorId(),
                VENDOR_VISIBLE_STATUSES,
                request.getKeyword(),
                request.getPageable()
        );

        Map<UUID, BigDecimal> fromPriceByTourId = loadFromPrices(tourPage.getContent());
        return PaginationUtils.toPaginationResponse(
                tourPage.map(tour -> toSummaryResponse(tour, fromPriceByTourId.get(tour.getTourId()))));
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
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    @Override
    @Transactional
    public TourDetailResponse createTour(String userEmail, CreateTourRequest request,
                                          MultipartFile coverImage, List<MultipartFile> tourImages) {
        Vendor vendor = resolveVendorByEmail(userEmail);
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Tour tour = tourMapper.toTour(request);
        tour.setStatus(TourStatus.APPROVED);
        tour.setVendor(vendor);
        tour.setCreator(creator);

        // Upload cover image
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverUrl = fileService.uploadFile(coverImage, "tours");
            tour.setCoverImageUrl(coverUrl);
        }

        tour = tourRepository.save(tour);

        // Upload tour gallery images (batch)
        List<TourImage> savedImages = new ArrayList<>();
        if (tourImages != null && !tourImages.isEmpty()) {
            List<String> imageUrls = fileService.uploadFiles(tourImages, "tours");
            for (int i = 0; i < imageUrls.size(); i++) {
                TourImage tourImage = new TourImage();
                tourImage.setTour(tour);
                tourImage.setImageUrl(imageUrls.get(i));
                tourImage.setSortOrder(i);
                savedImages.add(tourImage);
            }
            tourImageRepository.saveAll(savedImages);
        }

        return toDetailResponse(tour, savedImages, List.of(), List.of(), null);
    }

    @Override
    @Transactional
    public TourDetailResponse updateTour(String userEmail, UUID tourId, UpdateTourRequest request,
                                          MultipartFile coverImage, List<MultipartFile> tourImages) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (tour.getStatus() != TourStatus.PENDING_APPROVAL
                && tour.getStatus() != TourStatus.APPROVED
                && tour.getStatus() != TourStatus.HIDDEN
                && tour.getStatus() != TourStatus.DRAFT
                && tour.getStatus() != TourStatus.REJECTED) {
            throw new AppException(ErrorCode.TOUR_UPDATE_NOT_ALLOWED);
        }

        tourMapper.updateTourFromRequest(request, tour);

        // Upload cover image if provided
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverUrl = fileService.uploadFile(coverImage, "tours");
            tour.setCoverImageUrl(coverUrl);
        }

        tour = tourRepository.save(tour);

        // Smart replace tour gallery images:
        // - tourImages == null  → không gửi field → giữ nguyên ảnh cũ
        // - tourImages is empty → gửi mảng rỗng → xoá hết ảnh cũ
        // - tourImages has files → thay thế ảnh cũ bằng ảnh mới
        if (tourImages != null) {
            // Xoá tất cả ảnh cũ
            List<TourImage> existingImages = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
            if (!existingImages.isEmpty()) {
                tourImageRepository.deleteAll(existingImages);
            }

            // Upload ảnh mới (nếu có)
            if (!tourImages.isEmpty()) {
                List<String> imageUrls = fileService.uploadFiles(tourImages, "tours");
                List<TourImage> newImages = new ArrayList<>();
                for (int i = 0; i < imageUrls.size(); i++) {
                    TourImage tourImage = new TourImage();
                    tourImage.setTour(tour);
                    tourImage.setImageUrl(imageUrls.get(i));
                    tourImage.setSortOrder(i);
                    newImages.add(tourImage);
                }
                tourImageRepository.saveAll(newImages);
            }
        }

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    @Override
    @Transactional
    public void deleteTour(String userEmail, UUID tourId) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        // Tour ở APPROVED hoặc HIDDEN mới cần kiểm tra nhóm ghép đang hoạt động
        // DRAFT / REJECTED: xóa tự do
        if (tour.getStatus() == TourStatus.APPROVED || tour.getStatus() == TourStatus.HIDDEN) {
            if (matchingGroupRepository.existsByTour_TourIdAndStatusInAndIsDeletedFalse(tourId, ACTIVE_GROUP_STATUSES)) {
                throw new AppException(ErrorCode.TOUR_HAS_ACTIVE_GROUPS);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        tour.setIsDeleted(true);
        tour.setDeletedAt(now);
        tour.setDeletedBy(userEmail);
        tourRepository.save(tour);

        // Cascade: bulk update các bảng con — chỉ xóa những record chưa bị xóa trước đó
        tourCheckpointRepository.softDeleteByTourId(tourId, now, userEmail);
        tourScheduleRepository.softDeleteByTourId(tourId, now, userEmail);
        tourImageRepository.softDeleteByTourId(tourId, now, userEmail);
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
        tour.setRejectionReason(null);
        tour = tourRepository.save(tour);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    @Override
    @Transactional
    public TourDetailResponse approveTour(String userEmail, UUID tourId) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (tour.getStatus() != TourStatus.PENDING_APPROVAL) {
            throw new AppException(ErrorCode.TOUR_NOT_PENDING_APPROVAL);
        }

        tour.setStatus(TourStatus.APPROVED);
        tour.setRejectionReason(null);
        tour = tourRepository.save(tour);

        // Tự động mở (OPEN) tất cả các lịch khởi hành (Schedule) đang ở trạng thái CLOSED của Tour này
        List<TourSchedule> pendingSchedules = tourScheduleRepository
                .findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        for (TourSchedule schedule : pendingSchedules) {
            if (schedule.getStatus() == ScheduleStatus.CLOSED) {
                schedule.setStatus(ScheduleStatus.OPEN);
                tourScheduleRepository.save(schedule);
            }
        }

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    @Override
    @Transactional
    public TourDetailResponse rejectTour(String userEmail, UUID tourId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
        }
        String normalizedReason = reason.trim();

        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (tour.getStatus() != TourStatus.PENDING_APPROVAL) {
            throw new AppException(ErrorCode.TOUR_NOT_PENDING_APPROVAL);
        }

        tour.setStatus(TourStatus.REJECTED);
        tour.setRejectionReason(normalizedReason);
        tour = tourRepository.save(tour);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
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

        // Không cho phép ẩn nếu tour đang có nhóm ghép hoạt động
        if (matchingGroupRepository.existsByTour_TourIdAndStatusInAndIsDeletedFalse(tourId, ACTIVE_GROUP_STATUSES)) {
            throw new AppException(ErrorCode.TOUR_HAS_ACTIVE_GROUPS);
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
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    @Override
    @Transactional
    public TourDetailResponse revertTour(String userEmail, UUID tourId) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (tour.getStatus() != TourStatus.REJECTED) {
            throw new AppException(ErrorCode.TOUR_NOT_IN_REJECTED_STATUS);
        }

        tour.setStatus(TourStatus.PENDING_APPROVAL);
        tour = tourRepository.save(tour);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    @Override
    @Transactional
    public TourDetailResponse restoreTour(String userEmail, UUID tourId) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedTrue(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_DELETED));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        // Lưu lại deletedAt của tour để restore đúng đợt
        LocalDateTime deletedAt = tour.getDeletedAt();

        // Restore Tour cha
        tour.setIsDeleted(false);
        tour.setDeletedAt(null);
        tour.setDeletedBy(null);
        tour.setStatus(TourStatus.PENDING_APPROVAL); // Về PENDING_APPROVAL để xem, sửa và duyệt lại
        tour = tourRepository.save(tour);

        // Restore các bảng con bị xóa cùng đợt (match exact deletedAt)
        tourCheckpointRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);
        tourScheduleRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);
        tourImageRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }

    @Override
    @Transactional
    public TourDetailResponse unhideTour(String userEmail, UUID tourId) {
        Vendor vendor = resolveVendorByEmail(userEmail);

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (tour.getStatus() != TourStatus.HIDDEN) {
            throw new AppException(ErrorCode.TOUR_NOT_HIDDEN);
        }

        tour.setStatus(TourStatus.APPROVED);
        tour = tourRepository.save(tour);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);

        return toDetailResponse(tour, images, checkpoints, schedules, minPriceOf(schedules));
    }
}
