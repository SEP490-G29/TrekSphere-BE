package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.VendorApplicationFilterRequest;
import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorApplicationResponse;

public interface VendorApplicationService {
    VendorApplicationResponse submitApplication(String applicantEmail, VendorApplicationRequest request);
    PaginationResponse<VendorApplicationResponse> getApplications(VendorApplicationFilterRequest request);
}
