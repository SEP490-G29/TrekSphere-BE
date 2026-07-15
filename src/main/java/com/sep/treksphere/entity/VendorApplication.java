package com.sep.treksphere.entity;

import com.sep.treksphere.enums.vendor.ApplicationStatus;
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

    @Column(nullable = false, length = 255)
    private String companyName;

    @Column(nullable = false, length = 255)
    private String contactEmail;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String businessDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ApplicationStatus applicationStatus = ApplicationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(nullable = false, unique = true, length = 50)
    private String taxCode;

    @Column(nullable = false, length = 500)
    private String businessLicenseUrl;
}
