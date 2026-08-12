package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.booking.RefundMethod;
import com.sep.treksphere.enums.booking.RefundReason;
import com.sep.treksphere.enums.booking.RefundStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RefundTransactionResponse {
    private UUID refundTransactionId;
    private UUID bookingId;
    private UUID paymentTransactionId;
    private BigDecimal amount;
    private RefundReason reason;
    private String reasonDetail;
    private RefundStatus status;
    private RefundMethod refundMethod;
    private String destinationBin;
    private String maskedDestinationAccountNumber;
    private String destinationAccountName;
    private String gatewayRefundId;
    private LocalDateTime requestedAt;
    private LocalDateTime processingAt;
    private LocalDateTime completedAt;
    private String failureCode;
    private String failureMessage;
}
