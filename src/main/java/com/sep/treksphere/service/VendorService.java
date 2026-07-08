package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorResponse;

public interface VendorService {
    PaginationResponse<VendorResponse> getVendors(BaseFilterRequest request);
}
