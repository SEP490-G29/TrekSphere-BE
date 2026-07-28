package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.PublicVoucherFilterRequest;
import com.sep.treksphere.dto.request.VoucherFilterRequest;
import com.sep.treksphere.dto.response.VoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VoucherService {
    Page<VoucherResponse> getVendorVouchers(UUID vendorId, PublicVoucherFilterRequest request);
    
    Page<VoucherResponse> getVouchersForVendorUser(UUID userId, VoucherFilterRequest request);
}
