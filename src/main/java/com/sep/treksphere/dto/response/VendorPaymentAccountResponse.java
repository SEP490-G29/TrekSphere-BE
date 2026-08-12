package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import com.sep.treksphere.enums.booking.PaymentProvider;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VendorPaymentAccountResponse {
    private UUID vendorPaymentAccountId;
    private PaymentProvider provider;
    private String clientId;
    private boolean credentialsConfigured;
    private PaymentAccountStatus status;
    private String webhookUrl;
}
