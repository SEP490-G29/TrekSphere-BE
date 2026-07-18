package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.VendorProfileUpdateRequest;
import com.sep.treksphere.dto.request.VendorStatusUpdateRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorProfileResponse;
import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.enums.vendor.VendorStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.VendorMapper;
import com.sep.treksphere.repository.VendorApplicationRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.FileService;
import com.sep.treksphere.service.VendorService;
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
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final VendorApplicationRepository vendorApplicationRepository;
    private final FileService fileService;
    private final VendorMapper vendorMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<VendorResponse> getVendors(BaseFilterRequest request) {
        Page<Vendor> vendorsPage = vendorRepository.findByKeyword(
                request.getKeyword(),
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

    @Override
    @Transactional(readOnly = true)
    public VendorProfileResponse getVendorProfile(CustomUserDetails userDetails) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Fetching vendor profile for user ID: {}", userId);

        boolean isManager = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VENDOR_MANAGER"));
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VENDOR_STAFF"));

        Vendor vendor;

        if (isManager) {
            vendor = vendorRepository.findByManager_UserId(userId)
                    .orElseThrow(() -> {
                        log.error("Vendor not found for manager user ID: {}", userId);
                        return new AppException(ErrorCode.VENDOR_NOT_FOUND);
                    });
        } else if (isStaff) {
            VendorStaff staff = vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(userId)
                    .orElseThrow(() -> {
                        log.warn("Vendor staff user ID {} is not active staff of any vendor", userId);
                        return new AppException(ErrorCode.UNAUTHORIZED_VENDOR_ACCESS);
                    });
            vendor = staff.getVendor();
            if (vendor == null || vendor.getIsDeleted()) {
                log.error("Vendor linked to staff record of user ID {} does not exist or is deleted", userId);
                throw new AppException(ErrorCode.VENDOR_NOT_FOUND);
            }
        } else {
            log.warn("User ID {} with unauthorized roles tried to access vendor profile", userId);
            throw new AppException(ErrorCode.UNAUTHORIZED_VENDOR_ACCESS);
        }

        return vendorMapper.toVendorProfileResponse(vendor);
    }

    @Override
    @Transactional
    public VendorProfileResponse updateVendorProfile(CustomUserDetails userDetails, VendorProfileUpdateRequest request) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Updating vendor profile for user ID: {}", userId);

        Vendor vendor = vendorRepository.findByManager_UserId(userId)
                .orElseThrow(() -> {
                    log.error("Vendor not found for manager user ID: {}", userId);
                    return new AppException(ErrorCode.VENDOR_NOT_FOUND);
                });

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
        if (StringUtils.hasText(request.getBankAccount())) {
            vendor.setBankAccount(request.getBankAccount().trim());
        }
        if (StringUtils.hasText(request.getBankName())) {
            vendor.setBankName(request.getBankName().trim());
        }

        if (request.getLogo() != null && !request.getLogo().isEmpty()) {
            log.info("Uploading new logo for vendor ID: {}", vendor.getVendorId());
            String logoUrl = fileService.uploadFile(request.getLogo(), "vendor-logos");
            vendor.setLogoUrl(logoUrl);
        }

        if (request.getPaymentQr() != null && !request.getPaymentQr().isEmpty()) {
            log.info("Uploading new payment QR for vendor ID: {}", vendor.getVendorId());
            String qrUrl = fileService.uploadFile(request.getPaymentQr(), "vendor-qrs");
            vendor.setPaymentQrUrl(qrUrl);
        }

        vendor = vendorRepository.save(vendor);
        log.info("Successfully updated vendor profile for Vendor ID: {}", vendor.getVendorId());

        return vendorMapper.toVendorProfileResponse(vendor);
    }

    @Override
    @Transactional
    public VendorResponse updateVendorStatus(UUID id, VendorStatusUpdateRequest request) {
        log.info("Updating status for vendor ID: {} to {}", id, request.getStatus());
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Vendor not found with ID: {}", id);
                    return new AppException(ErrorCode.VENDOR_NOT_FOUND);
                });

        if (vendor.getStatus() == VendorStatus.REVOKED) {
            log.warn("Cannot change status for already REVOKED vendor ID: {}", id);
            throw new AppException(ErrorCode.VENDOR_REVOKED_STATUS);
        }

        vendor.setStatus(request.getStatus());
        vendor = vendorRepository.save(vendor);
        log.info("Successfully updated status for vendor ID: {}", id);

        return vendorMapper.toVendorResponse(vendor);
    }
}
