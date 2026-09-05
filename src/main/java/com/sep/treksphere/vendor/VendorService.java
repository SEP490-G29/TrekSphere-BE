package com.sep.treksphere.vendor;

import com.sep.treksphere.user.User;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.vendor.application.VendorApplicationRepository;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorApplicationRepository vendorApplicationRepository;
    private final FileService fileService;
    private final VendorMapper vendorMapper;
    private final VendorAccessService vendorAccessService;
    private final TourRepository tourRepository;

    @Transactional(readOnly = true)
    public PaginationResponse<VendorResponse> getVendors(VendorFilterRequest request) {
        Page<Vendor> vendorsPage = vendorRepository.findByKeywordAndStatus(
                request.getKeyword(),
                request.getStatus(),
                request.getPageable()
        );

        List<VendorResponse> responses = vendorsPage.getContent().stream()
                .map(vendorMapper::toVendorResponse)
                .toList();

        return PaginationResponse.<VendorResponse>builder()
                .content(responses)
                .pageNumber(vendorsPage.getNumber())
                .pageSize(vendorsPage.getSize())
                .totalElements(vendorsPage.getTotalElements())
                .totalPages(vendorsPage.getTotalPages())
                .last(vendorsPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public VendorProfileResponse getVendorProfile(CustomUserDetails userDetails) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Fetching vendor profile for user ID: {}", userId);

        Vendor vendor = vendorAccessService.resolveByManagerEmail(userDetails.getUsername());

        return vendorMapper.toVendorProfileResponse(vendor);
    }

    @Transactional
    public VendorProfileResponse updateVendorProfile(CustomUserDetails userDetails, VendorProfileUpdateRequest request) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Updating vendor profile for user ID: {}", userId);

        Vendor vendor = vendorAccessService.resolveActiveByManagerEmail(userDetails.getUsername());

        if (StringUtils.hasText(request.getContactEmail())) {
            String newEmail = request.getContactEmail().trim();
            if (!newEmail.equalsIgnoreCase(vendor.getContactEmail())) {
                boolean existsInVendors = vendorRepository.existsByContactEmailAndVendorIdNot(newEmail, vendor.getVendorId());
                boolean existsInApps = vendorApplicationRepository.existsByContactEmailAndApplicant_UserIdNot(newEmail, userId);
                if (existsInVendors || existsInApps) {
                    log.warn("Contact email {} is already registered during update of vendor profile {}", newEmail, vendor.getVendorId());
                    throw new AppException(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS);
                }
                vendor.setContactEmail(newEmail);
            }
        }

        if (StringUtils.hasText(request.getContactPhone())) {
            String newPhone = request.getContactPhone().trim();
            if (!newPhone.equals(vendor.getContactPhone())) {
                boolean existsInVendors = vendorRepository.existsByContactPhoneAndVendorIdNot(newPhone, vendor.getVendorId());
                boolean existsInApps = vendorApplicationRepository.existsByContactPhoneAndApplicant_UserIdNot(newPhone, userId);
                if (existsInVendors || existsInApps) {
                    log.warn("Contact phone {} is already registered during update of vendor profile {}", newPhone, vendor.getVendorId());
                    throw new AppException(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS);
                }
                vendor.setContactPhone(newPhone);
            }
        }

        if (StringUtils.hasText(request.getDescription())) {
            vendor.setDescription(request.getDescription().trim());
        }
        if (request.getBusinessAddress() != null) {
            vendor.setBusinessAddress(normalizeNullable(request.getBusinessAddress()));
        }
        if (request.getWebsiteUrl() != null) {
            vendor.setWebsiteUrl(normalizeNullable(request.getWebsiteUrl()));
        }
        if (request.getLogo() != null && !request.getLogo().isEmpty()) {
            log.info("Uploading new logo for vendor ID: {}", vendor.getVendorId());
            String logoUrl = fileService.uploadFile(request.getLogo(), "vendor-logos");
            vendor.setLogoUrl(logoUrl);
        }

        vendor = vendorRepository.save(vendor);
        log.info("Successfully updated vendor profile for Vendor ID: {}", vendor.getVendorId());

        return vendorMapper.toVendorProfileResponse(vendor);
    }

    @Transactional
    public VendorResponse updateVendorStatus(UUID id, VendorStatusUpdateRequest request) {
        log.info("Updating status for vendor ID: {} to {}", id, request.getStatus());
        Vendor vendor = vendorRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> {
                    log.error("Vendor not found with ID: {}", id);
                    return new AppException(ErrorCode.VENDOR_NOT_FOUND);
                });

        VendorStatus currentStatus = vendor.getStatus();
        VendorStatus targetStatus = request.getStatus();
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new AppException(ErrorCode.INVALID_VENDOR_STATUS_TRANSITION);
        }

        vendor.setStatus(targetStatus);
        vendor = vendorRepository.save(vendor);
        log.info("Successfully updated status for vendor ID: {}", id);

        return vendorMapper.toVendorResponse(vendor);
    }

    @Transactional(readOnly = true)
    public PublicVendorProfileResponse getPublicVendorProfile(UUID vendorId) {
        Vendor vendor = vendorRepository
                .findByVendorIdAndStatusAndIsDeletedFalse(vendorId, VendorStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));
        return PublicVendorProfileResponse.builder()
                .vendorId(vendor.getVendorId())
                .companyName(vendor.getCompanyName())
                .description(vendor.getDescription())
                .logoUrl(vendor.getLogoUrl())
                .businessAddress(vendor.getBusinessAddress())
                .websiteUrl(vendor.getWebsiteUrl())
                .contactEmail(vendor.getContactEmail())
                .contactPhone(vendor.getContactPhone())
                .partnerSince(vendor.getCreatedAt())
                .publishedTourCount(tourRepository.countByVendorVendorIdAndStatusAndIsDeletedFalse(
                        vendorId, TourStatus.PUBLISHED))
                .build();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
