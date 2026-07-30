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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.sep.treksphere.dto.request.CreateVoucherRequest;
import com.sep.treksphere.dto.request.UpdateVoucherRequest;
import com.sep.treksphere.enums.voucher.DiscountType;
import com.sep.treksphere.enums.voucher.VoucherStatus;
import com.sep.treksphere.entity.Voucher;

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

    @Override
    @Transactional
    public VoucherResponse createVoucher(UUID userId, CreateVoucherRequest request) {
        UUID vendorId = resolveVendorId(userId);
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

        if (voucherRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS);
        }

        if (!request.getValidFrom().isBefore(request.getValidUntil())) {
            throw new AppException(ErrorCode.VOUCHER_VALID_DATE_ERROR);
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.INVALID_DISCOUNT_VALUE);
        }

        Voucher voucher = voucherMapper.toVoucher(request);
        voucher.setVendor(vendor);
        voucher.setUsedCount(0);
        voucher.setStatus(VoucherStatus.ACTIVE);
        
        voucher = voucherRepository.save(voucher);
        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(UUID userId, UUID voucherId, UpdateVoucherRequest request) {
        UUID vendorId = resolveVendorId(userId);

        Voucher voucher = voucherRepository.findByVoucherIdAndVendor_VendorId(voucherId, vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        voucherMapper.updateVoucherFromRequest(request, voucher);

        if (!voucher.getValidFrom().isBefore(voucher.getValidUntil())) {
            throw new AppException(ErrorCode.VOUCHER_VALID_DATE_ERROR);
        }

        if (voucher.getDiscountType() == DiscountType.PERCENTAGE
                && voucher.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.INVALID_DISCOUNT_VALUE);
        }

        // If status is updated back to ACTIVE or CANCELLED, reset isDeleted to false
        if (voucher.getStatus() != VoucherStatus.INACTIVE) {
            voucher.setIsDeleted(false);
        } else {
            voucher.setIsDeleted(true);
        }

        voucher = voucherRepository.save(voucher);
        return voucherMapper.toVoucherResponse(voucher);
    }

    @Override
    @Transactional
    public void deleteVoucher(UUID userId, UUID voucherId) {
        UUID vendorId = resolveVendorId(userId);

        Voucher voucher = voucherRepository.findByVoucherIdAndVendor_VendorId(voucherId, vendorId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        voucher.setIsDeleted(true);
        voucher.setStatus(VoucherStatus.INACTIVE);
        voucherRepository.save(voucher);
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
