package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.TourCheckpointRequest;
import com.sep.treksphere.dto.response.TourCheckpointResponse;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourCheckpoint;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.TourCheckpointRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.TourCheckpointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourCheckpointServiceImpl implements TourCheckpointService {

    private final TourCheckpointRepository tourCheckpointRepository;
    private final TourRepository tourRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TourCheckpointResponse> getCheckpointsByTourId(UUID tourId) {
        Tour tour = tourRepository.findById(tourId)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        List<TourCheckpoint> checkpoints = tourCheckpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);

        return checkpoints.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public TourCheckpointResponse createCheckpoint(UUID tourId, TourCheckpointRequest request, String userEmail) {
        Tour tour = tourRepository.findById(tourId)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(tour, vendor);

        if (tourCheckpointRepository.existsByTourAndCheckpointOrderAndIsDeletedFalse(tour, request.getCheckpointOrder())) {
            throw new AppException(ErrorCode.CHECKPOINT_DUPLICATE_ORDER);
        }

        TourCheckpoint checkpoint = new TourCheckpoint();
        checkpoint.setTour(tour);
        checkpoint.setCheckpointName(request.getCheckpointName());
        checkpoint.setDescription(request.getDescription());
        checkpoint.setLatitude(request.getLatitude());
        checkpoint.setLongitude(request.getLongitude());
        checkpoint.setAltitude(request.getAltitude());
        checkpoint.setCheckpointOrder(request.getCheckpointOrder());
        checkpoint.setCheckpointImageUrl(request.getCheckpointImageUrl());

        return toResponse(tourCheckpointRepository.save(checkpoint));
    }

    @Override
    @Transactional
    public TourCheckpointResponse updateCheckpoint(UUID checkpointId, TourCheckpointRequest request, String userEmail) {
        TourCheckpoint checkpoint = tourCheckpointRepository.findById(checkpointId)
                .filter(cp -> !cp.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.CHECKPOINT_NOT_FOUND));

        Vendor vendor = resolveVendorByUser(userEmail);
        validateTourBelongsToVendor(checkpoint.getTour(), vendor);

        if (tourCheckpointRepository.existsByTourAndCheckpointOrderAndCheckpointIdNotAndIsDeletedFalse(
                checkpoint.getTour(), request.getCheckpointOrder(), checkpointId)) {
            throw new AppException(ErrorCode.CHECKPOINT_DUPLICATE_ORDER);
        }

        checkpoint.setCheckpointName(request.getCheckpointName());
        checkpoint.setDescription(request.getDescription());
        checkpoint.setLatitude(request.getLatitude());
        checkpoint.setLongitude(request.getLongitude());
        checkpoint.setAltitude(request.getAltitude());
        checkpoint.setCheckpointOrder(request.getCheckpointOrder());
        checkpoint.setCheckpointImageUrl(request.getCheckpointImageUrl());

        return toResponse(tourCheckpointRepository.save(checkpoint));
    }

    @Override
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
     * Resolve vendor from user email.
     * Supports both VendorManager (direct owner) and VendorStaff.
     */
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

    private TourCheckpointResponse toResponse(TourCheckpoint checkpoint) {
        return TourCheckpointResponse.builder()
                .checkpointId(checkpoint.getCheckpointId().toString())
                .tourId(checkpoint.getTour().getTourId().toString())
                .checkpointName(checkpoint.getCheckpointName())
                .description(checkpoint.getDescription())
                .latitude(checkpoint.getLatitude())
                .longitude(checkpoint.getLongitude())
                .altitude(checkpoint.getAltitude())
                .checkpointOrder(checkpoint.getCheckpointOrder())
                .checkpointImageUrl(checkpoint.getCheckpointImageUrl())
                .build();
    }
}
