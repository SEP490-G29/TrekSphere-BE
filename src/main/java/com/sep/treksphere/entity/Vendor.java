package com.sep.treksphere.entity;

import com.sep.treksphere.enums.vendor.VendorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "vendor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vendorID;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id", unique = true, nullable = false)
    private User managerUser;

    @Column(nullable = false, length = 255)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false, length = 255)
    private String contactEmail;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    @Column(length = 50)
    private String bankAccount;

    @Column(length = 100)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private VendorStatus status = VendorStatus.ACTIVE;
}
