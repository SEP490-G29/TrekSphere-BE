package com.sep.treksphere.entity;

import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import com.sep.treksphere.enums.booking.PaymentProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_payment_account")
@Getter
@Setter
@NoArgsConstructor
public class VendorPaymentAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vendorPaymentAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(nullable = false, length = 255)
    private String providerChannelId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String checksumKeyEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentAccountStatus onboardingStatus = PaymentAccountStatus.PENDING;

    @Column(nullable = false)
    private Boolean isDefault = true;

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
