package com.sep.treksphere.vendor.application.dto.response;

import com.sep.treksphere.user.UserResponse;
import com.sep.treksphere.vendor.application.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorApplicationResponse {
    private UUID vendorApplicationId;
    private UserResponse applicant;
    private String companyName;
    private String contactEmail;
    private String contactPhone;
    private String businessDescription;
    private ApplicationStatus applicationStatus;
    private String rejectionReason;
    private String taxCode;
    private String businessLicenseUrl;
    private String businessAddress;
    private String legalRepresentativeName;
    private String legalRepresentativePosition;
    private String websiteUrl;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private UUID vendorId;
    private LocalDateTime createdAt;
}
