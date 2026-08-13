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
    private String destinationAccountNumber;
    private String maskedDestinationAccountNumber;
    private String destinationAccountName;
    private String gatewayRefundId;
    private String manualBankReference;
    private LocalDateTime requestedAt;
    private LocalDateTime processingAt;
    private LocalDateTime completedAt;
    private LocalDateTime dueAt;
    private LocalDateTime nextRetryAt;
    private Integer attemptCount;
    private String failureCode;
    private String failureMessage;
    private String bookingCode;
    private String vendorName;
    private boolean automaticPayoutAvailable;
    private String manualReceiptUrl;
    private LocalDateTime manualSubmittedAt;
    private LocalDateTime adminReviewedAt;
    private String adminReviewNote;
}
