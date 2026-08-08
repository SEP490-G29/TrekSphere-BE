package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.CancellationPolicyRequest;
import com.sep.treksphere.dto.response.CancellationPolicyResponse;

import java.util.List;
import java.util.UUID;

public interface CancellationPolicyService {
    List<CancellationPolicyResponse> getVendorPolicies(String email);
    List<CancellationPolicyResponse> getPublicPoliciesByVendorId(UUID vendorId);
    CancellationPolicyResponse createPolicy(String email, CancellationPolicyRequest request);
    CancellationPolicyResponse updatePolicy(String email, UUID policyId, CancellationPolicyRequest request);
    void deletePolicy(String email, UUID policyId);
}
