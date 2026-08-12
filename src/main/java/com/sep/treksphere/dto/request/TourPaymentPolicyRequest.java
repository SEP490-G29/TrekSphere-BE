package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.booking.DepositType;
import com.sep.treksphere.enums.booking.PaymentOption;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TourPaymentPolicyRequest {
    @NotNull
    private PaymentOption paymentOption;
    private DepositType depositType;
    private BigDecimal depositValue;
    private Integer remainingDueDaysBeforeDeparture;
}
