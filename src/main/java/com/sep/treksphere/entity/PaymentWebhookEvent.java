package com.sep.treksphere.entity;

import com.sep.treksphere.enums.booking.PaymentProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_event")
@Getter
@Setter
@NoArgsConstructor
public class PaymentWebhookEvent {
    public enum ProcessingStatus { RECEIVED, PROCESSED, IGNORED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentWebhookEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(nullable = false, length = 600)
    private String gatewayEventKey;

    private Long gatewayOrderCode;
    private String gatewayPaymentLinkId;
    private String gatewayReference;

    @Column(nullable = false)
    private String signature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingStatus processingStatus = ProcessingStatus.RECEIVED;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) receivedAt = LocalDateTime.now();
    }
}
