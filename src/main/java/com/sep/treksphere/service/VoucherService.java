package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.PublicVoucherFilterRequest;
import com.sep.treksphere.dto.request.VoucherFilterRequest;
import com.sep.treksphere.dto.response.VoucherResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

import com.sep.treksphere.dto.request.CreateVoucherRequest;
import com.sep.treksphere.dto.request.UpdateVoucherRequest;
import com.sep.treksphere.dto.request.ValidateVoucherRequest;
import com.sep.treksphere.dto.response.VoucherValidationResponse;

public interface VoucherService {
    Page<VoucherResponse> getVendorVouchers(UUID vendorId, PublicVoucherFilterRequest request);
    
    Page<VoucherResponse> getVouchersForVendorUser(UUID userId, VoucherFilterRequest request);

    VoucherResponse createVoucher(UUID userId, CreateVoucherRequest request);

    VoucherResponse updateVoucher(UUID userId, UUID voucherId, UpdateVoucherRequest request);

    void deleteVoucher(UUID userId, UUID voucherId);

    VoucherValidationResponse validateVoucher(ValidateVoucherRequest request);
}
