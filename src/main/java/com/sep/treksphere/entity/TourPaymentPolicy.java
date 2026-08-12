package com.sep.treksphere.entity;

import com.sep.treksphere.enums.booking.DepositType;
import com.sep.treksphere.enums.booking.PaymentOption;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tour_payment_policy")
@Getter
@Setter
@NoArgsConstructor
public class TourPaymentPolicy extends BaseEntity {

    @Id
    private UUID tourId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentOption paymentOption = PaymentOption.FULL_PAYMENT_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DepositType depositType;

    @Column(precision = 12, scale = 2)
    private BigDecimal depositValue;

    private Integer remainingDueDaysBeforeDeparture;

    @Column(nullable = false)
    private Integer policyVersion = 1;

    @Column(nullable = false)
    private Boolean isActive = true;
}
