package com.sep.treksphere.entity;

import com.sep.treksphere.enums.voucher.DiscountType;
import com.sep.treksphere.enums.voucher.VoucherApprovalStatus;
import com.sep.treksphere.enums.voucher.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "voucher")
@Getter
@Setter
@NoArgsConstructor


public class Voucher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID voucherID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private Integer maxUsage;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoucherApprovalStatus approvalStatus = VoucherApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoucherStatus status = VoucherStatus.ACTIVE;
}
