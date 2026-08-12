package com.sep.treksphere.service.impl;

import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.dto.request.PayOsAccountConfigRequest;
import com.sep.treksphere.dto.request.TourPaymentPolicyRequest;
import com.sep.treksphere.dto.response.TourPaymentPolicyResponse;
import com.sep.treksphere.dto.response.VendorPaymentAccountResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.*;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.VendorPaymentConfigurationService;
import com.sep.treksphere.service.payment.PaymentCredentialCipher;
import com.sep.treksphere.service.payment.PayOsClientFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorPaymentConfigurationServiceImpl implements VendorPaymentConfigurationService {

    private final VendorRepository vendorRepository;
    private final TourRepository tourRepository;
    private final VendorPaymentAccountRepository accountRepository;
    private final TourPaymentPolicyRepository policyRepository;
    private final PayOsClientFactory clientFactory;
    private final PaymentCredentialCipher credentialCipher;
    private final PaymentWorkflowProperties properties;

    @Override
    @Transactional
    public VendorPaymentAccountResponse configurePayOsAccount(String email, PayOsAccountConfigRequest request) {
        Vendor vendor = requireManagedVendor(email);
        VendorPaymentAccount account = accountRepository
                .findByVendor_VendorIdAndProviderAndIsDefaultTrueAndIsDeletedFalse(vendor.getVendorId(), PaymentProvider.PAYOS)
                .orElseGet(VendorPaymentAccount::new);
        account.setVendor(vendor);
        account.setProvider(PaymentProvider.PAYOS);
        account.setProviderChannelId(request.getClientId().trim());
        account.setApiKeyEncrypted(credentialCipher.encrypt(request.getApiKey().trim()));
        account.setChecksumKeyEncrypted(credentialCipher.encrypt(request.getChecksumKey().trim()));
        account.setIsDefault(true);
        account.setIsDeleted(false);
        account.setOnboardingStatus(PaymentAccountStatus.PENDING);

        var client = clientFactory.getClient(account);
        String webhookUrl = webhookUrl(account.getProviderChannelId());
        if (webhookUrl == null) {
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED,
                    "PAYMENT_WEBHOOK_BASE_URL chưa được cấu hình bằng HTTPS public URL.");
        }
        try {
            client.webhooks().confirm(webhookUrl);
        } catch (Exception exception) {
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "Không thể xác minh credential hoặc đăng ký webhook payOS.");
        }
        account.setOnboardingStatus(PaymentAccountStatus.ACTIVE);
        return toAccountResponse(accountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public VendorPaymentAccountResponse getPayOsAccount(String email) {
        Vendor vendor = requireManagedVendor(email);
        VendorPaymentAccount account = accountRepository
                .findByVendor_VendorIdAndProviderAndIsDefaultTrueAndIsDeletedFalse(vendor.getVendorId(), PaymentProvider.PAYOS)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED));
        return toAccountResponse(account);
    }

    @Override
    @Transactional
    public TourPaymentPolicyResponse updateTourPaymentPolicy(String email, UUID tourId, TourPaymentPolicyRequest request) {
        Vendor vendor = requireManagedVendor(email);
        Tour tour = tourRepository.findById(tourId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        if (!tour.getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }
        validatePolicy(request);
        TourPaymentPolicy policy = policyRepository.findById(tourId).orElseGet(TourPaymentPolicy::new);
        policy.setTour(tour);
        policy.setPaymentOption(request.getPaymentOption());
        policy.setDepositType(request.getDepositType());
        policy.setDepositValue(request.getDepositValue());
        policy.setRemainingDueDaysBeforeDeparture(request.getRemainingDueDaysBeforeDeparture());
        policy.setPolicyVersion(policy.getTourId() == null ? 1 : policy.getPolicyVersion() + 1);
        policy.setIsActive(true);
        policy.setIsDeleted(false);
        return toPolicyResponse(policyRepository.save(policy));
    }

    @Override
    @Transactional(readOnly = true)
    public TourPaymentPolicyResponse getTourPaymentPolicy(String email, UUID tourId) {
        Vendor vendor = requireManagedVendor(email);
        TourPaymentPolicy policy = policyRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_PLAN_NOT_ALLOWED));
        if (!policy.getTour().getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }
        return toPolicyResponse(policy);
    }

    private void validatePolicy(TourPaymentPolicyRequest request) {
        if (request.getPaymentOption() == PaymentOption.FULL_PAYMENT_ONLY) {
            if (request.getDepositType() != null || request.getDepositValue() != null
                    || request.getRemainingDueDaysBeforeDeparture() != null) {
                throw new AppException(ErrorCode.PAYMENT_PLAN_NOT_ALLOWED,
                        "FULL_PAYMENT_ONLY không được cấu hình thông tin đặt cọc.");
            }
            return;
        }
        if (request.getDepositType() == null || request.getDepositValue() == null
                || request.getRemainingDueDaysBeforeDeparture() == null
                || request.getRemainingDueDaysBeforeDeparture() < 0) {
            throw new AppException(ErrorCode.PAYMENT_PLAN_NOT_ALLOWED, "Thiếu cấu hình đặt cọc.");
        }
        BigDecimal value = request.getDepositValue();
        boolean invalid = value.signum() <= 0
                || request.getDepositType() == DepositType.PERCENTAGE
                    && value.compareTo(BigDecimal.valueOf(100)) >= 0;
        if (invalid) throw new AppException(ErrorCode.PAYMENT_PLAN_NOT_ALLOWED, "Giá trị đặt cọc không hợp lệ.");
    }

    private Vendor requireManagedVendor(String email) {
        return vendorRepository.findByManager_Email(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    private VendorPaymentAccountResponse toAccountResponse(VendorPaymentAccount account) {
        return VendorPaymentAccountResponse.builder()
                .vendorPaymentAccountId(account.getVendorPaymentAccountId())
                .provider(account.getProvider())
                .clientId(account.getProviderChannelId())
                .credentialsConfigured(account.getApiKeyEncrypted() != null
                        && account.getChecksumKeyEncrypted() != null)
                .status(account.getOnboardingStatus())
                .webhookUrl(webhookUrl(account.getProviderChannelId()))
                .build();
    }

    private TourPaymentPolicyResponse toPolicyResponse(TourPaymentPolicy policy) {
        return TourPaymentPolicyResponse.builder()
                .tourId(policy.getTourId())
                .paymentOption(policy.getPaymentOption())
                .depositType(policy.getDepositType())
                .depositValue(policy.getDepositValue())
                .remainingDueDaysBeforeDeparture(policy.getRemainingDueDaysBeforeDeparture())
                .policyVersion(policy.getPolicyVersion())
                .build();
    }

    private String webhookUrl(String channelId) {
        String base = properties.getWebhookBaseUrl();
        if (base == null || base.isBlank()) return null;
        return base.replaceAll("/+$", "") + "/" + channelId;
    }
}
