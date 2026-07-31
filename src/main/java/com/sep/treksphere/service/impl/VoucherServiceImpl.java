package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.*;
import com.sep.treksphere.dto.response.VoucherResponse;
import com.sep.treksphere.dto.response.VoucherValidationResponse;
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
import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.voucher.DiscountType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public VoucherValidationResponse validateVoucher(ValidateVoucherRequest request) {
        Optional<Voucher> optionalVoucher = voucherRepository.findByCodeAndIsDeletedFalse(request.getCode());

        if (optionalVoucher.isEmpty()) {
            return VoucherValidationResponse.builder()
                    .isValid(false)
                    .message(MessageConstant.VOUCHER_NOT_FOUND)
                    .build();
        }

        Voucher voucher = optionalVoucher.get();

        if (request.getVendorId() != null && !request.getVendorId().equals(voucher.getVendor().getVendorId())) {
            return VoucherValidationResponse.builder()
                    .isValid(false)
                    .message(MessageConstant.VOUCHER_VENDOR_MISMATCH)
                    .build();
        }

        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            return VoucherValidationResponse.builder()
                    .isValid(false)
                    .message(MessageConstant.VOUCHER_NOT_ACTIVE)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getValidFrom())) {
            return VoucherValidationResponse.builder()
                    .isValid(false)
                    .message(MessageConstant.VOUCHER_NOT_YET_VALID)
                    .build();
        }
        if (now.isAfter(voucher.getValidUntil())) {
            return VoucherValidationResponse.builder()
                    .isValid(false)
                    .message(MessageConstant.VOUCHER_EXPIRED)
                    .build();
        }

        if (voucher.getMaxUsage() != null && voucher.getUsedCount() >= voucher.getMaxUsage()) {
            return VoucherValidationResponse.builder()
                    .isValid(false)
                    .message(MessageConstant.VOUCHER_MAX_USAGE_REACHED)
                    .build();
        }

        if (request.getOrderValue().compareTo(voucher.getMinOrderValue()) < 0) {
            return VoucherValidationResponse.builder()
                    .isValid(false)
                    .message(MessageConstant.VOUCHER_MIN_ORDER_VALUE_NOT_MET)
                    .build();
        }

        BigDecimal discountAmount;
        if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = voucher.getDiscountValue();
            if (discountAmount.compareTo(request.getOrderValue()) > 0) {
                discountAmount = request.getOrderValue();
            }
        } else {
            discountAmount = request.getOrderValue().multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
        }

        return VoucherValidationResponse.builder()
                .isValid(true)
                .discountAmount(discountAmount)
                .message(MessageConstant.VOUCHER_VALIDATION_SUCCESS)
                .build();
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
