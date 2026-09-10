package com.sep.treksphere.vendor;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.user.User;
import com.sep.treksphere.vendor.VendorStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "vendor")
@Getter
@Setter
@NoArgsConstructor


public class Vendor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vendorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", unique = true, nullable = false)
    private User manager;

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

    @Column(nullable = false, unique = true, length = 50)
    private String taxCode;

    @Column(nullable = false, length = 500)
    private String businessLicenseUrl;

    @Column(length = 500)
    private String businessAddress;

    @Column(length = 255)
    private String legalRepresentativeName;

    @Column(length = 255)
    private String legalRepresentativePosition;

    @Column(length = 500)
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VendorStatus status = VendorStatus.ACTIVE;
}
