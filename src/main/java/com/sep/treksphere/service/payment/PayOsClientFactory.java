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

    public PayOS getPayoutClient(VendorPaymentAccount account) {
        if (account == null || account.getPayoutStatus() != com.sep.treksphere.enums.booking.PaymentAccountStatus.ACTIVE
                || isBlank(account.getPayoutProviderChannelId())
                || isBlank(account.getPayoutApiKeyEncrypted())
                || isBlank(account.getPayoutChecksumKeyEncrypted())) {
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED,
                    "Vendor chưa cấu hình Kênh Chi payOS hoạt động");
        }
        return new PayOS(
                account.getPayoutProviderChannelId(),
                credentialCipher.decrypt(account.getPayoutApiKeyEncrypted()),
                credentialCipher.decrypt(account.getPayoutChecksumKeyEncrypted()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
