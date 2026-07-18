package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.VendorProfileUpdateRequest;
import com.sep.treksphere.dto.request.VendorStatusUpdateRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorProfileResponse;
import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface VendorService {
    PaginationResponse<VendorResponse> getVendors(BaseFilterRequest request);
    VendorProfileResponse getVendorProfile(CustomUserDetails userDetails);
    VendorProfileResponse updateVendorProfile(CustomUserDetails userDetails, VendorProfileUpdateRequest request);
    VendorResponse updateVendorStatus(UUID id, VendorStatusUpdateRequest request);
}
