package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorPayoutAccountResponse {
    private boolean configured;
    private String clientId;
    private boolean credentialsConfigured;
    private PaymentAccountStatus status;
    private String maskedAccountNumber;
    private String accountName;
}
