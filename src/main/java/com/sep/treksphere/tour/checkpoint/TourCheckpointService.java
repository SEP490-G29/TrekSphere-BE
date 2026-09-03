package com.sep.treksphere.tour.checkpoint;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourCheckpointService {

    private final TourCheckpointRepository tourCheckpointRepository;
    private final TourRepository tourRepository;
    private final VendorRepository vendorRepository;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public List<TourCheckpointResponse> getCheckpointsByTourId(UUID tourId) {
        Tour tour = tourRepository.findById(tourId)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        List<TourCheckpoint> checkpoints = tourCheckpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);

        return checkpoints.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TourCheckpointResponse createCheckpoint(UUID tourId, TourCheckpointRequest request, List<MultipartFile> images, String userEmail) {
        Tour tour = tourRepository.findById(tourId)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(tour, vendor);

        // Validations
        if (tourCheckpointRepository.existsByTourAndCheckpointOrderAndIsDeletedFalse(tour, request.getCheckpointOrder())) {
            throw new AppException(ErrorCode.CHECKPOINT_DUPLICATE_ORDER);
        }

        if (tourCheckpointRepository.existsByTourAndCheckpointNameIgnoreCaseAndIsDeletedFalse(tour, request.getCheckpointName().trim())) {
            throw new AppException(ErrorCode.CHECKPOINT_DUPLICATE_NAME);
        }

        if (request.getLatitude() != null && request.getLongitude() != null) {
            if (tourCheckpointRepository.existsByTourAndLatitudeAndLongitudeAndIsDeletedFalse(tour, request.getLatitude(), request.getLongitude())) {
                throw new AppException(ErrorCode.CHECKPOINT_DUPLICATE_COORDINATES);
            }
        }

        TourCheckpoint checkpoint = new TourCheckpoint();
        checkpoint.setTour(tour);
        checkpoint.setCheckpointName(request.getCheckpointName().trim());
        checkpoint.setDescription(request.getDescription());
        checkpoint.setLatitude(request.getLatitude());
        checkpoint.setLongitude(request.getLongitude());
        checkpoint.setAltitude(request.getAltitude());
        checkpoint.setCheckpointOrder(request.getCheckpointOrder());

        // Process images
        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = fileService.uploadFiles(images, "checkpoints");
            checkpoint.setCheckpointImageUrl(String.join(",", imageUrls));
        } else if (request.getCheckpointImageUrl() != null) {
            checkpoint.setCheckpointImageUrl(request.getCheckpointImageUrl());
        }

        return toResponse(tourCheckpointRepository.save(checkpoint));
    }

    @Transactional
    public TourCheckpointResponse updateCheckpoint(UUID checkpointId, TourCheckpointRequest request, List<MultipartFile> images, String userEmail) {
        TourCheckpoint checkpoint = tourCheckpointRepository.findById(checkpointId)
                .filter(cp -> !cp.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.CHECKPOINT_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(checkpoint.getTour(), vendor);

        Tour tour = checkpoint.getTour();

        // Validations
        if (tourCheckpointRepository.existsByTourAndCheckpointOrderAndTourCheckpointIdNotAndIsDeletedFalse(
                tour, request.getCheckpointOrder(), checkpointId)) {
            throw new AppException(ErrorCode.CHECKPOINT_DUPLICATE_ORDER);
        }

        if (tourCheckpointRepository.existsByTourAndCheckpointNameIgnoreCaseAndTourCheckpointIdNotAndIsDeletedFalse(
                tour, request.getCheckpointName().trim(), checkpointId)) {
            throw new AppException(ErrorCode.CHECKPOINT_DUPLICATE_NAME);
        }

        if (request.getLatitude() != null && request.getLongitude() != null) {
            if (tourCheckpointRepository.existsByTourAndLatitudeAndLongitudeAndTourCheckpointIdNotAndIsDeletedFalse(
                    tour, request.getLatitude(), request.getLongitude(), checkpointId)) {
                throw new AppException(ErrorCode.CHECKPOINT_DUPLICATE_COORDINATES);
            }
        }

        checkpoint.setCheckpointName(request.getCheckpointName().trim());
        checkpoint.setDescription(request.getDescription());
        checkpoint.setLatitude(request.getLatitude());
        checkpoint.setLongitude(request.getLongitude());
        checkpoint.setAltitude(request.getAltitude());
        checkpoint.setCheckpointOrder(request.getCheckpointOrder());

        // Process images
        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = fileService.uploadFiles(images, "checkpoints");
            checkpoint.setCheckpointImageUrl(String.join(",", imageUrls));
        } else if (request.getCheckpointImageUrl() != null) {
            checkpoint.setCheckpointImageUrl(request.getCheckpointImageUrl());
        }

        return toResponse(tourCheckpointRepository.save(checkpoint));
    }

    @Transactional
    public void deleteCheckpoint(UUID checkpointId, String userEmail) {
        TourCheckpoint checkpoint = tourCheckpointRepository.findById(checkpointId)
                .filter(cp -> !cp.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.CHECKPOINT_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(checkpoint.getTour(), vendor);

        checkpoint.setIsDeleted(true);
        checkpoint.setDeletedAt(LocalDateTime.now());
        checkpoint.setDeletedBy(userEmail);
        tourCheckpointRepository.save(checkpoint);
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

    private TourCheckpointResponse toResponse(TourCheckpoint checkpoint) {
        String rawUrls = checkpoint.getCheckpointImageUrl();
        List<String> imageUrlList = (rawUrls != null && !rawUrls.isBlank())
                ? List.of(rawUrls.split(","))
                : List.of();

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
                .checkpointImageUrls(imageUrlList)
                .build();
    }
}
