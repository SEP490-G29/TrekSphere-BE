package com.sep.treksphere.service.payment;

import com.sep.treksphere.entity.VendorPaymentAccount;
import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import com.sep.treksphere.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOsClientFactoryTest {

    @Mock
    private PaymentCredentialCipher credentialCipher;

    @InjectMocks
    private PayOsClientFactory factory;

    @Test
    void payoutClientUsesOnlyPayoutChannelCredentials() {
        VendorPaymentAccount account = new VendorPaymentAccount();
        account.setProviderChannelId("collection-client");
        account.setApiKeyEncrypted("collection-api");
        account.setChecksumKeyEncrypted("collection-checksum");
        account.setPayoutProviderChannelId("payout-client");
        account.setPayoutApiKeyEncrypted("payout-api");
        account.setPayoutChecksumKeyEncrypted("payout-checksum");
        account.setPayoutStatus(PaymentAccountStatus.ACTIVE);
        when(credentialCipher.decrypt("payout-api")).thenReturn("plain-payout-api");
        when(credentialCipher.decrypt("payout-checksum")).thenReturn("plain-payout-checksum");

        assertNotNull(factory.getPayoutClient(account));

        verify(credentialCipher).decrypt("payout-api");
        verify(credentialCipher).decrypt("payout-checksum");
        verify(credentialCipher, never()).decrypt("collection-api");
        verify(credentialCipher, never()).decrypt("collection-checksum");
    }

    @Test
    void payoutClientRejectsInactivePayoutChannelEvenWhenCollectionChannelExists() {
        VendorPaymentAccount account = new VendorPaymentAccount();
        account.setProviderChannelId("collection-client");
        account.setApiKeyEncrypted("collection-api");
        account.setChecksumKeyEncrypted("collection-checksum");

        assertThrows(AppException.class, () -> factory.getPayoutClient(account));

        verify(credentialCipher, never()).decrypt("collection-api");
        verify(credentialCipher, never()).decrypt("collection-checksum");
    }
}
