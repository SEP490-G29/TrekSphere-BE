package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.booking.DepositType;
import com.sep.treksphere.enums.booking.PaymentOption;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TourPaymentPolicyResponse {
    private UUID tourId;
    private PaymentOption paymentOption;
    private DepositType depositType;
    private BigDecimal depositValue;
    private Integer remainingDueDaysBeforeDeparture;
    private Integer policyVersion;
}
