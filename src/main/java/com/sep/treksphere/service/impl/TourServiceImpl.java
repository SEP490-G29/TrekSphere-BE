package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.CreateTourRequest;
import com.sep.treksphere.dto.request.UpdateTourRequest;
import com.sep.treksphere.dto.request.TourParticipationPolicyRequest;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.entity.CancellationPolicy;
import com.sep.treksphere.entity.Notification;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourCheckpoint;
import com.sep.treksphere.entity.TourImage;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.TourParticipationPolicy;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.blog.ReviewStatus;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.enums.tour.TourStatus;
import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import com.sep.treksphere.enums.booking.PaymentProvider;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.TourMapper;
import com.sep.treksphere.repository.CancellationPolicyRepository;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.ReviewRepository;
import com.sep.treksphere.repository.TourCheckpointRepository;
import com.sep.treksphere.repository.TourImageRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.TourPaymentPolicyRepository;
import com.sep.treksphere.repository.TourParticipationPolicyRepository;
import com.sep.treksphere.repository.VendorPaymentAccountRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;
    private final TourCheckpointRepository tourCheckpointRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourPaymentPolicyRepository tourPaymentPolicyRepository;
    private final TourParticipationPolicyRepository tourParticipationPolicyRepository;
    private final VendorPaymentAccountRepository vendorPaymentAccountRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
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

        return PaginationUtils.toPaginationResponse(tourPage.map(this::toSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse getTourById(UUID tourId) {
        Tour tour = tourRepository.findDetailById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);
        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
    }

    private TourSummaryResponse toSummaryResponse(Tour tour) {
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);
        BookingReadiness readiness = getBookingReadiness(tour);

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
                .onlineBookingEnabled(readiness.enabled())
                .onlineBookingDisabledReason(readiness.disabledReason())
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .createdAt(tour.getCreatedAt())
                .build();
    }

    private TourDetailResponse toDetailResponse(
            Tour tour,
            List<TourImage> images,
            List<TourCheckpoint> checkpoints,
            List<TourSchedule> schedules,
            Double avgRating,
            int totalReviews) {
        List<CancellationPolicy> policies = (tour.getVendor() != null)
                ? cancellationPolicyRepository.findByVendorAndIsActiveTrueAndIsDeletedFalseOrderByCancelBeforeDaysDesc(tour.getVendor())
                : List.of();
        List<CancellationPolicyResponse> policyResponses = policies.stream()
                .map(this::toPolicyResponse)
                .toList();
        TourPaymentPolicyResponse paymentPolicy = tourPaymentPolicyRepository
                .findByTourIdAndIsActiveTrueAndIsDeletedFalse(tour.getTourId())
                .map(policy -> TourPaymentPolicyResponse.builder()
                        .tourId(policy.getTourId())
                        .paymentOption(policy.getPaymentOption())
                        .depositType(policy.getDepositType())
                        .depositValue(policy.getDepositValue())
                        .remainingDueDaysBeforeDeparture(policy.getRemainingDueDaysBeforeDeparture())
                        .policyVersion(policy.getPolicyVersion())
                        .build())
                .orElse(null);
        TourParticipationPolicyResponse participationPolicy = tourParticipationPolicyRepository
                .findByTourIdAndIsActiveTrueAndIsDeletedFalse(tour.getTourId())
                .map(this::toParticipationPolicyResponse)
                .orElse(null);
        BookingReadiness readiness = getBookingReadiness(tour);

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
                // Cancellation policies
                .cancellationPolicies(policyResponses)
                // Payment/refund policy displayed before creating a booking
                .paymentPolicy(paymentPolicy)
                .participationPolicy(participationPolicy)
                .onlineBookingEnabled(readiness.enabled())
                .onlineBookingDisabledReason(readiness.disabledReason())
                .nonRefundableCost(tour.getNonRefundableCost())
                // Review stats
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .build();
    }

    private CancellationPolicyResponse toPolicyResponse(CancellationPolicy policy) {
        return CancellationPolicyResponse.builder()
                .cancellationPolicyId(policy.getCancellationPolicyId() != null ? policy.getCancellationPolicyId().toString() : null)
                .cancelBeforeDays(policy.getCancelBeforeDays())
                .refundPercentage(policy.getRefundPercentage())
                .description(policy.getDescription())
                .isActive(policy.getIsActive())
                .build();
    }

    private BookingReadiness getBookingReadiness(Tour tour) {
        if (tour.getStatus() != TourStatus.APPROVED) {
            return new BookingReadiness(false, "Tour chưa được duyệt để nhận đặt online.");
        }
        boolean hasPayOsAccount = vendorPaymentAccountRepository
                .existsByVendor_VendorIdAndProviderAndOnboardingStatusAndIsDefaultTrueAndIsDeletedFalse(
                        tour.getVendor().getVendorId(), PaymentProvider.PAYOS, PaymentAccountStatus.ACTIVE);
        if (!hasPayOsAccount) {
            return new BookingReadiness(false, "Nhà tổ chức chưa hoàn tất kết nối payOS.");
        }
        if (!tourPaymentPolicyRepository.existsByTourIdAndIsActiveTrueAndIsDeletedFalse(tour.getTourId())) {
            return new BookingReadiness(false, "Tour chưa có chính sách thanh toán.");
        }
        if (!tourParticipationPolicyRepository.existsByTourIdAndIsActiveTrueAndIsDeletedFalse(tour.getTourId())) {
            return new BookingReadiness(false, "Tour chưa có điều kiện tham gia.");
        }
        return new BookingReadiness(true, null);
    }

    private record BookingReadiness(boolean enabled, String disabledReason) {}

    private TourParticipationPolicyResponse toParticipationPolicyResponse(TourParticipationPolicy policy) {
        return TourParticipationPolicyResponse.builder()
                .tourId(policy.getTourId())
                .policyVersion(policy.getPolicyVersion())
                .minAge(policy.getMinAge() == null ? null : policy.getMinAge().intValue())
                .maxAge(policy.getMaxAge() == null ? null : policy.getMaxAge().intValue())
                .minHeightCm(policy.getMinHeightCm())
                .maxHeightCm(policy.getMaxHeightCm())
                .minWeightKg(policy.getMinWeightKg())
                .maxWeightKg(policy.getMaxWeightKg())
                .fitnessLevel(policy.getFitnessLevel())
                .healthRequirements(policy.getHealthRequirements())
                .restrictedMedicalConditions(policy.getRestrictedMedicalConditions())
                .requiredExperience(policy.getRequiredExperience())
                .requiredSkills(policy.getRequiredSkills())
                .requiredEquipment(policy.getRequiredEquipment())
                .requiredDocuments(policy.getRequiredDocuments())
                .requiresHealthDeclaration(policy.getRequiresHealthDeclaration())
                .requiresMedicalCertificate(policy.getRequiresMedicalCertificate())
                .guardianRequiredUnderAge(policy.getGuardianRequiredUnderAge() == null
                        ? null : policy.getGuardianRequiredUnderAge().intValue())
                .additionalRequirements((String) policy.getAdditionalRules().get("notes"))
                .build();
    }

    private void saveParticipationPolicy(Tour tour, TourParticipationPolicyRequest request) {
        if (request == null) return;
        validateParticipationPolicy(request);

        TourParticipationPolicy policy = tourParticipationPolicyRepository.findById(tour.getTourId())
                .orElseGet(TourParticipationPolicy::new);
        boolean existing = policy.getTourId() != null;
        policy.setTour(tour);
        policy.setPolicyVersion(existing ? policy.getPolicyVersion() + 1 : 1);
        policy.setMinAge(request.getMinAge() == null ? null : request.getMinAge().shortValue());
        policy.setMaxAge(request.getMaxAge() == null ? null : request.getMaxAge().shortValue());
        policy.setMinHeightCm(request.getMinHeightCm());
        policy.setMaxHeightCm(request.getMaxHeightCm());
        policy.setMinWeightKg(request.getMinWeightKg());
        policy.setMaxWeightKg(request.getMaxWeightKg());
        policy.setFitnessLevel(request.getFitnessLevel());
        policy.setHealthRequirements(trimToNull(request.getHealthRequirements()));
        policy.setRestrictedMedicalConditions(trimToNull(request.getRestrictedMedicalConditions()));
        policy.setRequiredExperience(trimToNull(request.getRequiredExperience()));
        policy.setRequiredSkills(trimToNull(request.getRequiredSkills()));
        policy.setRequiredEquipment(trimToNull(request.getRequiredEquipment()));
        policy.setRequiredDocuments(trimToNull(request.getRequiredDocuments()));
        policy.setRequiresHealthDeclaration(!Boolean.FALSE.equals(request.getRequiresHealthDeclaration()));
        policy.setRequiresMedicalCertificate(Boolean.TRUE.equals(request.getRequiresMedicalCertificate()));
        policy.setGuardianRequiredUnderAge(request.getGuardianRequiredUnderAge() == null
                ? null : request.getGuardianRequiredUnderAge().shortValue());
        Map<String, Object> rules = new HashMap<>();
        if (StringUtils.hasText(request.getAdditionalRequirements())) {
            rules.put("notes", request.getAdditionalRequirements().trim());
        }
        policy.setAdditionalRules(rules);
        policy.setIsActive(true);
        policy.setIsDeleted(false);
        tourParticipationPolicyRepository.save(policy);
    }

    private void validateParticipationPolicy(TourParticipationPolicyRequest request) {
        if (request.getMinAge() != null && request.getMaxAge() != null
                && request.getMinAge() > request.getMaxAge()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tuổi tối thiểu không được lớn hơn tuổi tối đa.");
        }
        if (request.getMinHeightCm() != null && request.getMaxHeightCm() != null
                && request.getMinHeightCm().compareTo(request.getMaxHeightCm()) > 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Chiều cao tối thiểu không được lớn hơn chiều cao tối đa.");
        }
        if (request.getMinWeightKg() != null && request.getMaxWeightKg() != null
                && request.getMinWeightKg().compareTo(request.getMaxWeightKg()) > 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Cân nặng tối thiểu không được lớn hơn cân nặng tối đa.");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private TourCheckpointResponse toCheckpointResponse(TourCheckpoint checkpoint) {
        String rawUrls = checkpoint.getCheckpointImageUrl();
        List<String> imageUrlList = (rawUrls != null && !rawUrls.isBlank())
                ? List.of(rawUrls.split(","))
                : List.of();

        return TourCheckpointResponse.builder()
                .checkpointId(checkpoint.getCheckpointId().toString())
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
                .imageId(image.getImageId().toString())
                .imageUrl(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .caption(image.getCaption())
                .build();
    }

    private TourScheduleResponse toScheduleResponse(TourSchedule schedule) {
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

        Page<Tour> tourPage;
        boolean isManager = vendorRepository.findByManager_Email(userEmail).isPresent();

        if (isManager) {
            // Manager thấy: PENDING_APPROVAL, APPROVED, HIDDEN, REJECTED — không thấy DRAFT của staff
            java.util.List<TourStatus> managerStatuses = java.util.List.of(
                    TourStatus.PENDING_APPROVAL,
                    TourStatus.APPROVED,
                    TourStatus.HIDDEN,
                    TourStatus.REJECTED
            );
            tourPage = tourRepository.findByVendorIdForManager(
                    vendor.getVendorId(),
                    managerStatuses,
                    request.getKeyword(),
                    request.getPageable()
            );
        } else {
            // Staff: thấy DRAFT, REJECTED của chính mình và APPROVED, HIDDEN của Vendor (để tạo Schedule)
            User staffUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            tourPage = tourRepository.findByVendorIdForStaff(
                    vendor.getVendorId(),
                    staffUser.getUserId(),
                    request.getKeyword(),
                    request.getPageable()
            );
        }

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
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
    }

    @Override
    @Transactional
    public TourDetailResponse createTour(String userEmail, CreateTourRequest request,
                                          MultipartFile coverImage, List<MultipartFile> tourImages) {
        Vendor vendor = resolveVendorByEmail(userEmail);
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Tour tour = tourMapper.toTour(request);

        // Manager tạo → APPROVED
        // Staff tạo → DRAFT
        boolean isManager = vendorRepository.findByManager_Email(userEmail).isPresent();
        tour.setStatus(isManager ? TourStatus.APPROVED : TourStatus.DRAFT);

        tour.setVendor(vendor);
        tour.setCreator(creator);

        // Upload cover image
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverUrl = fileService.uploadFile(coverImage, "tours");
            tour.setCoverImageUrl(coverUrl);
        }

        tour = tourRepository.save(tour);
        saveParticipationPolicy(tour, request.getParticipationPolicy());

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

        return toDetailResponse(tour, savedImages, List.of(), List.of(), 0.0, 0);
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

        // Phân quyền sửa theo role:
        // Manager: được cập nhật cả tour đang bán; booking cũ vẫn giữ policy snapshot.
        // Staff   : sửa được DRAFT, REJECTED hoặc HIDDEN
        boolean isManager = vendorRepository.findByManager_Email(userEmail).isPresent();
        if (isManager) {
            if (tour.getStatus() != TourStatus.PENDING_APPROVAL
                    && tour.getStatus() != TourStatus.APPROVED
                    && tour.getStatus() != TourStatus.HIDDEN) {
                throw new AppException(ErrorCode.TOUR_UPDATE_NOT_ALLOWED);
            }
        } else {
            if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.REJECTED && tour.getStatus() != TourStatus.HIDDEN) {
                throw new AppException(ErrorCode.TOUR_STATUS_NOT_EDITABLE);
            }
        }

        tourMapper.updateTourFromRequest(request, tour);

        // Upload cover image if provided
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverUrl = fileService.uploadFile(coverImage, "tours");
            tour.setCoverImageUrl(coverUrl);
        }

        tour = tourRepository.save(tour);
        saveParticipationPolicy(tour, request.getParticipationPolicy());

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
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
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

        // Tour ở APPROVED hoặc HIDDEN mới cần kiểm tra booking
        // DRAFT / REJECTED: xóa tự do
        if (tour.getStatus() == TourStatus.APPROVED || tour.getStatus() == TourStatus.HIDDEN) {
            if (bookingRepository.existsActiveBookingByTourId(tourId)) {
                throw new AppException(ErrorCode.TOUR_HAS_ACTIVE_BOOKINGS);
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
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
    }

    @Override
    @Transactional
    public TourDetailResponse approveTour(String userEmail, UUID tourId) {
        Vendor vendor = vendorRepository.findByManager_Email(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

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

        // Gửi thông báo cho người tạo tour (Staff hoặc Manager)
        User recipient = tour.getCreator() != null ? tour.getCreator() : vendor.getManager();
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle("Tour đã được phê duyệt");
        notification.setEventType(NotificationEventType.TOUR_APPROVED);
        notification.setContent("Tour \"" + tour.getTourName() + "\" đã được phê duyệt và sẵn sàng mở bán.");
        notification.setReferenceType(ReferenceType.TOUR);
        notification.setReferenceId(tour.getTourId());
        notificationRepository.save(notification);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
    }

    @Override
    @Transactional
    public TourDetailResponse rejectTour(String userEmail, UUID tourId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
        }
        String normalizedReason = reason.trim();

        Vendor vendor = vendorRepository.findByManager_Email(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

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

        // Gửi thông báo từ chối cho người tạo tour
        User recipient = tour.getCreator() != null ? tour.getCreator() : tour.getVendor().getManager();
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle("Tour bị từ chối phê duyệt");
        notification.setEventType(NotificationEventType.TOUR_REJECTED);
        notification.setContent("Tour \"" + tour.getTourName() + "\" đã bị từ chối. Lý do: " + normalizedReason);
        notification.setReferenceType(ReferenceType.TOUR);
        notification.setReferenceId(tour.getTourId());
        notificationRepository.save(notification);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository
                .findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
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

        // Không cho phép ẩn nếu tour đang có booking chưa huỷ
        if (bookingRepository.existsActiveBookingByTourId(tourId)) {
            throw new AppException(ErrorCode.TOUR_HAS_ACTIVE_BOOKINGS);
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
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
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

        // Manager revert → PENDING_APPROVAL (để Manager xem lại và duyệt)
        // Staff revert → DRAFT (tiếp tục chỉnh sửa nháp)
        boolean isManager = vendorRepository.findByManager_Email(userEmail).isPresent();
        tour.setStatus(isManager ? TourStatus.PENDING_APPROVAL : TourStatus.DRAFT);
        tour = tourRepository.save(tour);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
    }

    @Override
    @Transactional
    public TourDetailResponse restoreTour(String userEmail, UUID tourId) {
        Vendor vendor = vendorRepository.findByManager_Email(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

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
        tour.setStatus(TourStatus.PENDING_APPROVAL); // Về PENDING_APPROVAL để Manager xem, sửa và duyệt lại
        tour = tourRepository.save(tour);

        // Restore các bảng con bị xóa cùng đợt (match exact deletedAt)
        tourCheckpointRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);
        tourScheduleRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);
        tourImageRepository.restoreByTourIdAndDeletedAt(tourId, deletedAt);

        List<TourImage> images = tourImageRepository.findByTourOrderBySortOrderAsc(tour);
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        List<TourSchedule> schedules = tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour);
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
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
        Double avgRating = reviewRepository.findAverageRatingByTourAndStatus(tour, ReviewStatus.APPROVED);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, ReviewStatus.APPROVED);

        return toDetailResponse(tour, images, checkpoints, schedules, avgRating, totalReviews);
    }
}
