package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.CancellationPolicyRequest;
import com.sep.treksphere.dto.response.CancellationPolicyResponse;
import com.sep.treksphere.entity.CancellationPolicy;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.CancellationPolicyRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.service.CancellationPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancellationPolicyServiceImpl implements CancellationPolicyService {

    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final VendorRepository vendorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CancellationPolicyResponse> getVendorPolicies(String email) {
        Vendor vendor = getVendorByManagerEmail(email);
        List<CancellationPolicy> policies = cancellationPolicyRepository
                .findByVendorAndIsDeletedFalseOrderByCancelBeforeDaysDesc(vendor);
        return policies.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CancellationPolicyResponse> getPublicPoliciesByVendorId(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

        List<CancellationPolicy> policies = cancellationPolicyRepository
                .findByVendorAndIsActiveTrueAndIsDeletedFalseOrderByCancelBeforeDaysDesc(vendor);
        return policies.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CancellationPolicyResponse createPolicy(String email, CancellationPolicyRequest request) {
        Vendor vendor = getVendorByManagerEmail(email);

        if (cancellationPolicyRepository.existsByVendorAndCancelBeforeDaysAndIsDeletedFalse(vendor, request.getCancelBeforeDays())) {
            throw new AppException(ErrorCode.POLICY_DUPLICATE_DAYS);
        }

        CancellationPolicy policy = new CancellationPolicy();
        policy.setVendor(vendor);
        policy.setCancelBeforeDays(request.getCancelBeforeDays());
        policy.setRefundPercentage(request.getRefundPercentage());
        policy.setDescription(request.getDescription());
        policy.setIsActive(true);

        CancellationPolicy savedPolicy = cancellationPolicyRepository.save(policy);
        return toResponse(savedPolicy);
    }

    @Override
    @Transactional
    public CancellationPolicyResponse updatePolicy(String email, UUID policyId, CancellationPolicyRequest request) {
        Vendor vendor = getVendorByManagerEmail(email);

        CancellationPolicy policy = cancellationPolicyRepository.findByCancellationPolicyIdAndIsDeletedFalse(policyId)
                .orElseThrow(() -> new AppException(ErrorCode.POLICY_NOT_FOUND));

        if (!policy.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (cancellationPolicyRepository.existsByVendorAndCancelBeforeDaysAndCancellationPolicyIdNotAndIsDeletedFalse(
                vendor, request.getCancelBeforeDays(), policyId)) {
            throw new AppException(ErrorCode.POLICY_DUPLICATE_DAYS);
        }

        policy.setCancelBeforeDays(request.getCancelBeforeDays());
        policy.setRefundPercentage(request.getRefundPercentage());
        policy.setDescription(request.getDescription());

        CancellationPolicy updatedPolicy = cancellationPolicyRepository.save(policy);
        return toResponse(updatedPolicy);
    }

    @Override
    @Transactional
    public void deletePolicy(String email, UUID policyId) {
        Vendor vendor = getVendorByManagerEmail(email);

        CancellationPolicy policy = cancellationPolicyRepository.findByCancellationPolicyIdAndIsDeletedFalse(policyId)
                .orElseThrow(() -> new AppException(ErrorCode.POLICY_NOT_FOUND));

        if (!policy.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        policy.setIsActive(false);
        policy.setIsDeleted(true);
        cancellationPolicyRepository.save(policy);
    }

    private Vendor getVendorByManagerEmail(String email) {
        return vendorRepository.findByManager_Email(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private CancellationPolicyResponse toResponse(CancellationPolicy policy) {
        return CancellationPolicyResponse.builder()
                .cancellationPolicyId(policy.getCancellationPolicyId() != null ? policy.getCancellationPolicyId().toString() : null)
                .cancelBeforeDays(policy.getCancelBeforeDays())
                .refundPercentage(policy.getRefundPercentage())
                .description(policy.getDescription())
                .isActive(policy.getIsActive())
                .build();
    }
}
