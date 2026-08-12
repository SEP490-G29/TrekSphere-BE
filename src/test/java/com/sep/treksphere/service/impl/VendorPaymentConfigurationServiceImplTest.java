package com.sep.treksphere.service.impl;

import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.dto.request.PayOsAccountConfigRequest;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorPaymentAccount;
import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.TourPaymentPolicyRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.VendorPaymentAccountRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.service.payment.PayOsClientFactory;
import com.sep.treksphere.service.payment.PaymentCredentialCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;
import vn.payos.exception.UnauthorizedException;
import vn.payos.model.webhooks.ConfirmWebhookResponse;
import vn.payos.service.blocking.webhooks.WebhooksService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorPaymentConfigurationServiceImplTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private TourRepository tourRepository;
    @Mock private VendorPaymentAccountRepository accountRepository;
    @Mock private TourPaymentPolicyRepository policyRepository;
    @Mock private PayOsClientFactory clientFactory;
    @Mock private PaymentCredentialCipher credentialCipher;
    @Mock private PaymentWorkflowProperties properties;
    @Mock private PayOS payOS;
    @Mock private WebhooksService webhooks;

    @InjectMocks private VendorPaymentConfigurationServiceImpl service;

    private final String email = "vendor@example.com";
    private Vendor vendor;
    private PayOsAccountConfigRequest request;

    @BeforeEach
    void setUp() {
        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        request = new PayOsAccountConfigRequest();
        request.setClientId("client-1234");
        request.setApiKey("api-key");
        request.setChecksumKey("checksum-key");

        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.of(vendor));
        when(accountRepository.findByVendor_VendorIdAndProviderAndIsDefaultTrueAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(credentialCipher.encrypt("api-key")).thenReturn("v1:api");
        when(credentialCipher.encrypt("checksum-key")).thenReturn("v1:checksum");
        when(properties.getWebhookBaseUrl())
                .thenReturn("https://api.treksphere.io.vn/api/v1/payment-webhooks/payos");
        when(clientFactory.getClient(any(VendorPaymentAccount.class))).thenReturn(payOS);
        when(payOS.webhooks()).thenReturn(webhooks);
        when(accountRepository.saveAndFlush(any(VendorPaymentAccount.class)))
                .thenAnswer(invocation -> {
                    VendorPaymentAccount account = invocation.getArgument(0);
                    account.setVendorPaymentAccountId(UUID.randomUUID());
                    return account;
                });
    }

    @Test
    void validPayOsCredentialsActivateAndPersistAccount() {
        when(webhooks.confirm(anyString())).thenReturn(new ConfirmWebhookResponse());
        when(accountRepository.save(any(VendorPaymentAccount.class)))
                .thenAnswer(invocation -> {
                    VendorPaymentAccount account = invocation.getArgument(0);
                    account.setVendorPaymentAccountId(UUID.randomUUID());
                    return account;
                });

        var response = service.configurePayOsAccount(email, request);

        assertEquals(PaymentAccountStatus.ACTIVE, response.getStatus());
        assertEquals("client-1234", response.getClientId());
        verify(accountRepository).save(any(VendorPaymentAccount.class));
    }

    @Test
    void rejectedCredentialsReturnBusinessErrorInsteadOfCloudflare502() {
        when(webhooks.confirm(anyString()))
                .thenThrow(new UnauthorizedException("invalid credentials", 401, "401", "Unauthorized"));

        AppException exception = assertThrows(AppException.class,
                () -> service.configurePayOsAccount(email, request));

        assertEquals(ErrorCode.PAYMENT_ACCOUNT_VERIFICATION_FAILED, exception.getErrorCode());
        verify(accountRepository).delete(any(VendorPaymentAccount.class));
        verify(accountRepository, never()).save(any());
    }
}
