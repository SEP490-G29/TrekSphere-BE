package com.sep.treksphere.vendor.application;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.user.User;
import com.sep.treksphere.vendor.application.ApplicationStatus;
import com.sep.treksphere.vendor.Vendor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "vendor_application")
@Getter
@Setter
@NoArgsConstructor


public class VendorApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vendor_application_id")
    private UUID vendorApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Column(length = 255)
    private String companyName;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String businessDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ApplicationStatus applicationStatus = ApplicationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(unique = true, length = 50)
    private String taxCode;

    @Column(length = 500)
    private String businessLicenseUrl;

    @Column(length = 500)
    private String businessAddress;

    @Column(length = 255)
    private String legalRepresentativeName;

    @Column(length = 255)
    private String legalRepresentativePosition;

    @Column(length = 500)
    private String websiteUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private java.time.LocalDateTime reviewedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", unique = true)
    private Vendor vendor;
}
