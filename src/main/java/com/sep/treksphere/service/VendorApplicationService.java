package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.response.VendorApplicationResponse;

public interface VendorApplicationService {
    VendorApplicationResponse submitApplication(String applicantEmail, VendorApplicationRequest request);
}
