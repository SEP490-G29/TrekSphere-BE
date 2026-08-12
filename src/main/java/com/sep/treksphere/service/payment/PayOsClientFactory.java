package com.sep.treksphere.service.payment;

import com.sep.treksphere.entity.VendorPaymentAccount;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;

@Component
@RequiredArgsConstructor
public class PayOsClientFactory {

    private final PaymentCredentialCipher credentialCipher;

    public PayOS getClient(VendorPaymentAccount account) {
        if (account == null || isBlank(account.getProviderChannelId())
                || isBlank(account.getApiKeyEncrypted()) || isBlank(account.getChecksumKeyEncrypted())) {
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED);
        }
        return new PayOS(
                account.getProviderChannelId(),
                credentialCipher.decrypt(account.getApiKeyEncrypted()),
                credentialCipher.decrypt(account.getChecksumKeyEncrypted()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
