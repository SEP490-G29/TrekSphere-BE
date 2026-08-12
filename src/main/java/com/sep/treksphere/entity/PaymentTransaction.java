package com.sep.treksphere.entity;

import com.sep.treksphere.enums.booking.PaymentProvider;
import com.sep.treksphere.enums.booking.PaymentStage;
import com.sep.treksphere.enums.booking.PaymentTransactionStatus;
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
@Table(name = "payment_transaction")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_payment_account_id", nullable = false)
    private VendorPaymentAccount vendorPaymentAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStage paymentStage;

    @Column(nullable = false)
    private Short attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    private Long gatewayOrderCode;
    private String gatewayPaymentLinkId;
    private String gatewayReference;

    @Column(length = 1000)
    private String checkoutUrl;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gatewayFeeAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionStatus status = PaymentTransactionStatus.CREATED;

    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    @Column(length = 100)
    private String failureCode;

    @Column(length = 500)
    private String failureMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> gatewayMetadata = new HashMap<>();

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
