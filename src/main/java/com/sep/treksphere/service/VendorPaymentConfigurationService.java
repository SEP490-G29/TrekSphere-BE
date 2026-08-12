package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.PayOsAccountConfigRequest;
import com.sep.treksphere.dto.request.TourPaymentPolicyRequest;
import com.sep.treksphere.dto.response.TourPaymentPolicyResponse;
import com.sep.treksphere.dto.response.VendorPaymentAccountResponse;

import java.util.UUID;

public interface VendorPaymentConfigurationService {
    VendorPaymentAccountResponse configurePayOsAccount(String email, PayOsAccountConfigRequest request);
    VendorPaymentAccountResponse getPayOsAccount(String email);
    TourPaymentPolicyResponse updateTourPaymentPolicy(String email, UUID tourId, TourPaymentPolicyRequest request);
    TourPaymentPolicyResponse getTourPaymentPolicy(String email, UUID tourId);
}
