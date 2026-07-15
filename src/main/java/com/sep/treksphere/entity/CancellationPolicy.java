package com.sep.treksphere.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "cancellation_policy")
@Getter
@Setter
@NoArgsConstructor


public class CancellationPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID cancellationPolicyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private Integer cancelBeforeDays;

    @Column(nullable = false)
    private Integer refundPercentage;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true;
}
