package com.sep.treksphere.entity;

import com.sep.treksphere.enums.booking.RefundMethod;
import com.sep.treksphere.enums.booking.RefundReason;
import com.sep.treksphere.enums.booking.RefundStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "refund_transaction")
@Getter
@Setter
@NoArgsConstructor
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID refundTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    private PaymentTransaction paymentTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RefundReason reason;

    @Column(length = 500)
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status = RefundStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundMethod refundMethod = RefundMethod.MANUAL;

    @Column(length = 20)
    private String destinationBin;

    @Column(length = 50)
    private String destinationAccountNumber;
    private String destinationAccountName;
    private String gatewayRefundId;

    @Column(length = 100)
    private String manualBankReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> gatewayMetadata = new HashMap<>();

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime processingAt;
    private LocalDateTime completedAt;
    private LocalDateTime dueAt;
    private LocalDateTime nextRetryAt;
    private LocalDateTime manualSubmittedAt;
    private LocalDateTime adminReviewedAt;

    @Column(length = 500)
    private String manualReceiptUrl;

    @Column(length = 500)
    private String adminReviewNote;

    @Column(nullable = false)
    private Integer attemptCount = 0;

    @Column(length = 100)
    private String failureCode;

    @Column(length = 500)
    private String failureMessage;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        var now = LocalDateTime.now();
        if (requestedAt == null) requestedAt = now;
        if (dueAt == null) dueAt = now.plusHours(48);
        if (createdAt == null) createdAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
