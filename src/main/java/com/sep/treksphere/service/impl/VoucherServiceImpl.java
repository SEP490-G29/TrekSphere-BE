package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.PublicVoucherFilterRequest;
import com.sep.treksphere.dto.request.VoucherFilterRequest;
import com.sep.treksphere.dto.response.VoucherResponse;
import com.sep.treksphere.enums.voucher.VoucherStatus;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.VoucherMapper;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.repository.VoucherRepository;
import com.sep.treksphere.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final VoucherMapper voucherMapper;

    @Override
    public Page<VoucherResponse> getVendorVouchers(UUID vendorId, PublicVoucherFilterRequest request) {
        return voucherRepository.filterVendorVouchers(
                vendorId,
                request.getKeyword(),
                request.getDiscountType(),
                VoucherStatus.ACTIVE,
                null,
                null,
                null,
                request.getPageable()
        ).map(voucherMapper::toVoucherResponse);
    }

    @Override
    public Page<VoucherResponse> getVouchersForVendorUser(UUID userId, VoucherFilterRequest request) {
        UUID vendorId = resolveVendorId(userId);
        
        LocalDateTime validUntilStart = null;
        LocalDateTime validUntilEnd = null;
        if (request.getValidUntil() != null) {
            validUntilStart = request.getValidUntil().atStartOfDay();
            validUntilEnd = request.getValidUntil().atTime(23, 59, 59);
        }

        return voucherRepository.filterVendorVouchers(
                vendorId,
                request.getKeyword(),
                request.getDiscountType(),
                request.getStatus(),
                validUntilStart,
                validUntilEnd,
                request.getMaxUsage(),
                request.getPageable()
        ).map(voucherMapper::toVoucherResponse);
    }

    private UUID resolveVendorId(UUID userId) {
        Optional<Vendor> vendor = vendorRepository.findByManager_UserId(userId);
        if (vendor.isPresent()) {
            return vendor.get().getVendorId();
        }

        VendorStaff staff = vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_VENDOR_ACCESS));
        return staff.getVendor().getVendorId();
    }
}
