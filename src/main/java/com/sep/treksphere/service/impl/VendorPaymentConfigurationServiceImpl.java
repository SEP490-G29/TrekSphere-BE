package com.sep.treksphere.service.impl;

import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.dto.request.PayOsAccountConfigRequest;
import com.sep.treksphere.dto.request.TourPaymentPolicyRequest;
import com.sep.treksphere.dto.response.TourPaymentPolicyResponse;
import com.sep.treksphere.dto.response.VendorPaymentAccountResponse;
import com.sep.treksphere.dto.response.VendorPayoutAccountResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.*;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.VendorPaymentConfigurationService;
import com.sep.treksphere.service.payment.PaymentCredentialCipher;
import com.sep.treksphere.service.payment.PayOsClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.exception.APIException;
import vn.payos.exception.PayOSException;
import vn.payos.exception.WebhookException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorPaymentConfigurationServiceImpl implements VendorPaymentConfigurationService {

    private final VendorRepository vendorRepository;
    private final TourRepository tourRepository;
    private final VendorPaymentAccountRepository accountRepository;
    private final TourPaymentPolicyRepository policyRepository;
    private final PayOsClientFactory clientFactory;
    private final PaymentCredentialCipher credentialCipher;
    private final PaymentWorkflowProperties properties;

    @Override
    public VendorPaymentAccountResponse configurePayOsAccount(String email, PayOsAccountConfigRequest request) {
        Vendor vendor = requireManagedVendor(email);
        var existing = accountRepository
                .findByVendor_VendorIdAndProviderAndIsDefaultTrueAndIsDeletedFalse(vendor.getVendorId(), PaymentProvider.PAYOS)
                .orElse(null);
        VendorPaymentAccount account = existing != null ? existing : new VendorPaymentAccount();
        String previousChannelId = existing != null ? existing.getProviderChannelId() : null;
        String previousApiKey = existing != null ? existing.getApiKeyEncrypted() : null;
        String previousChecksumKey = existing != null ? existing.getChecksumKeyEncrypted() : null;
        PaymentAccountStatus previousStatus = existing != null ? existing.getOnboardingStatus() : null;

        account.setVendor(vendor);
        account.setProvider(PaymentProvider.PAYOS);
        account.setProviderChannelId(request.getClientId().trim());
        account.setApiKeyEncrypted(credentialCipher.encrypt(request.getApiKey().trim()));
        account.setChecksumKeyEncrypted(credentialCipher.encrypt(request.getChecksumKey().trim()));
        account.setIsDefault(true);
        account.setIsDeleted(false);
        account.setOnboardingStatus(PaymentAccountStatus.PENDING);

        String webhookUrl = webhookUrl(account.getProviderChannelId());
        if (webhookUrl == null) {
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED,
                    "PAYMENT_WEBHOOK_BASE_URL chưa được cấu hình bằng HTTPS public URL.");
        }

        // payOS confirms a webhook by calling it immediately. Persist PENDING
        // before that external call so the callback can load the channel and
        // verify its signature. With one outer transaction the callback cannot
        // see this row and incorrectly returns PAYMENT_ACCOUNT_NOT_CONFIGURED.
        account = accountRepository.saveAndFlush(account);

        try {
            var client = clientFactory.getClient(account);
            confirmPayOsWebhook(client, webhookUrl, account.getProviderChannelId());
        } catch (RuntimeException exception) {
            restoreAccountAfterFailedVerification(account, existing != null, previousChannelId,
                    previousApiKey, previousChecksumKey, previousStatus);
            throw exception;
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
    public VendorPayoutAccountResponse configurePayoutAccount(
            String email, PayOsAccountConfigRequest request) {
        Vendor vendor = requireManagedVendor(email);
        VendorPaymentAccount account = accountRepository
                .findByVendor_VendorIdAndProviderAndIsDefaultTrueAndIsDeletedFalse(
                        vendor.getVendorId(), PaymentProvider.PAYOS)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED,
                        "Cần kết nối Kênh Thu payOS trước khi thêm Kênh Chi."));

        account.setPayoutProviderChannelId(request.getClientId().trim());
        account.setPayoutApiKeyEncrypted(credentialCipher.encrypt(request.getApiKey().trim()));
        account.setPayoutChecksumKeyEncrypted(credentialCipher.encrypt(request.getChecksumKey().trim()));
        account.setPayoutStatus(PaymentAccountStatus.PENDING);

        try {
            var payoutClient = new vn.payos.PayOS(
                    account.getPayoutProviderChannelId(),
                    credentialCipher.decrypt(account.getPayoutApiKeyEncrypted()),
                    credentialCipher.decrypt(account.getPayoutChecksumKeyEncrypted()));
            var payoutInfo = payoutClient.payoutsAccount().balance();
            account.setPayoutAccountNumber(payoutInfo.getAccountNumber());
            account.setPayoutAccountName(payoutInfo.getAccountName());
        } catch (APIException exception) {
            int status = exception.getStatusCode().orElse(0);
            log.warn("payOS payout channel verification rejected suffix={} status={} code={}",
                    maskedSuffix(account.getPayoutProviderChannelId()), status,
                    exception.getErrorCode().orElse("unknown"));
            if (status >= 400 && status < 500 && status != 429) {
                throw new AppException(ErrorCode.PAYMENT_ACCOUNT_VERIFICATION_FAILED,
                        "Không thể xác minh Kênh Chi. Kiểm tra bộ khóa, kích hoạt Kênh Chi và IP được phép trên payOS.");
            }
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        } catch (PayOSException exception) {
            log.warn("payOS unavailable while verifying payout channel suffix={} type={}",
                    maskedSuffix(account.getPayoutProviderChannelId()),
                    exception.getClass().getSimpleName());
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }

        account.setPayoutStatus(PaymentAccountStatus.ACTIVE);
        return toPayoutResponse(accountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public VendorPayoutAccountResponse getPayoutAccount(String email) {
        Vendor vendor = requireManagedVendor(email);
        return accountRepository.findByVendor_VendorIdAndProviderAndIsDefaultTrueAndIsDeletedFalse(
                        vendor.getVendorId(), PaymentProvider.PAYOS)
                .map(this::toPayoutResponse)
                .orElseGet(() -> VendorPayoutAccountResponse.builder().configured(false).build());
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

    private VendorPayoutAccountResponse toPayoutResponse(VendorPaymentAccount account) {
        boolean configured = account.getPayoutProviderChannelId() != null
                && account.getPayoutStatus() != null;
        return VendorPayoutAccountResponse.builder()
                .configured(configured)
                .clientId(account.getPayoutProviderChannelId())
                .credentialsConfigured(account.getPayoutApiKeyEncrypted() != null
                        && account.getPayoutChecksumKeyEncrypted() != null)
                .status(account.getPayoutStatus())
                .maskedAccountNumber(mask(account.getPayoutAccountNumber()))
                .accountName(account.getPayoutAccountName())
                .build();
    }

    private void confirmPayOsWebhook(vn.payos.PayOS client, String webhookUrl, String channelId) {
        try {
            client.webhooks().confirm(webhookUrl);
        } catch (APIException exception) {
            int status = exception.getStatusCode().orElse(0);
            String providerCode = exception.getErrorCode().orElse("unknown");
            log.warn("payOS account verification rejected for channel suffix={} status={} code={}",
                    maskedSuffix(channelId), status, providerCode);
            if (status >= 400 && status < 500 && status != 429) {
                throw new AppException(ErrorCode.PAYMENT_ACCOUNT_VERIFICATION_FAILED);
            }
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        } catch (WebhookException exception) {
            log.warn("payOS webhook confirmation rejected for channel suffix={} type={}",
                    maskedSuffix(channelId), exception.getClass().getSimpleName());
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_VERIFICATION_FAILED);
        } catch (PayOSException exception) {
            log.warn("payOS unavailable while verifying channel suffix={} type={}",
                    maskedSuffix(channelId), exception.getClass().getSimpleName());
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    private String maskedSuffix(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.length() <= 4 ? "****" : "****" + value.substring(value.length() - 4);
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) return value;
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }

    private void restoreAccountAfterFailedVerification(
            VendorPaymentAccount account,
            boolean existed,
            String previousChannelId,
            String previousApiKey,
            String previousChecksumKey,
            PaymentAccountStatus previousStatus) {
        if (!existed) {
            accountRepository.delete(account);
            return;
        }
        account.setProviderChannelId(previousChannelId);
        account.setApiKeyEncrypted(previousApiKey);
        account.setChecksumKeyEncrypted(previousChecksumKey);
        account.setOnboardingStatus(previousStatus);
        accountRepository.save(account);
    }
}
