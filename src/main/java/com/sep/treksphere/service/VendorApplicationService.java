package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.VendorApplicationFilterRequest;
import com.sep.treksphere.dto.request.VendorApplicationRejectRequest;
import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.request.VendorApplicationUpdateRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorApplicationResponse;
import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface VendorApplicationService {
    VendorApplicationResponse saveDraftApplication(UUID applicantId, VendorApplicationRequest request);
    PaginationResponse<VendorApplicationResponse> getApplications(VendorApplicationFilterRequest request);
    VendorApplicationResponse getApplicationById(UUID id, CustomUserDetails userDetails);
    VendorResponse approveApplication(UUID id);
    VendorApplicationResponse rejectApplication(UUID id, VendorApplicationRejectRequest request);
    VendorApplicationResponse updateApplication(UUID id, VendorApplicationUpdateRequest request, UUID applicantId);
    VendorApplicationResponse submitDraftApplication(UUID id, UUID applicantId);
    VendorApplicationResponse resubmitRejectedApplication(UUID id, UUID applicantId);
}
