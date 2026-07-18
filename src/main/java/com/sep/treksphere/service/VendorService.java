package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorProfileResponse;
import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.security.CustomUserDetails;

public interface VendorService {
    PaginationResponse<VendorResponse> getVendors(BaseFilterRequest request);
    VendorProfileResponse getVendorProfile(CustomUserDetails userDetails);
}
