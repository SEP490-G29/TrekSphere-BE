package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorProfileResponse;
import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.VendorMapper;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.VendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
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
}
