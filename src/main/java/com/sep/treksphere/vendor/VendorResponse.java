package com.sep.treksphere.vendor;

import com.sep.treksphere.user.UserProfileResponse;
import com.sep.treksphere.vendor.VendorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorResponse {
    private String vendorId;
    private UserProfileResponse manager;
    private String companyName;
    private String description;
    private String logoUrl;
    private String contactEmail;
    private String contactPhone;
    private String taxCode;
    private String businessLicenseUrl;
    private String businessAddress;
    private String legalRepresentativeName;
    private String legalRepresentativePosition;
    private String websiteUrl;
    private VendorStatus status;
}
