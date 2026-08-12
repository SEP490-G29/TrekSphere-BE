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
public class PaymentTransactionResponse {
    private UUID paymentTransactionId;
    private PaymentStage paymentStage;
    private Short attemptNumber;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String currency;
    private PaymentTransactionStatus status;
    private Long orderCode;
    private String checkoutUrl;
    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;
    private String failureCode;
    private String failureMessage;
    /** PAYOS hoặc LEGACY_BANK_TRANSFER để FE phân biệt dữ liệu trước khi chuyển cổng. */
    private String source;
    private LocalDateTime createdAt;
}
