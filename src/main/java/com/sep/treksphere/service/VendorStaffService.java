package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.VendorStaffAddRequest;
import com.sep.treksphere.dto.request.VendorStaffStatusUpdateRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorStaffResponse;

import java.util.UUID;

public interface VendorStaffService {
    PaginationResponse<VendorStaffResponse> getMyVendorStaff(String managerEmail, BaseFilterRequest request);
    VendorStaffResponse addVendorStaff(String managerEmail, VendorStaffAddRequest request);
    VendorStaffResponse updateVendorStaffStatus(String managerEmail, UUID staffId, VendorStaffStatusUpdateRequest request);
}
