package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.booking.PaymentStage;
import com.sep.treksphere.enums.booking.PaymentTransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentCheckoutResponse {
    private UUID paymentTransactionId;
    private UUID bookingId;
    private PaymentStage paymentStage;
    private BigDecimal amount;
    private String currency;
    private PaymentTransactionStatus status;
    private Long orderCode;
    private String checkoutUrl;
    private String qrCode;
    private LocalDateTime expiredAt;
}
