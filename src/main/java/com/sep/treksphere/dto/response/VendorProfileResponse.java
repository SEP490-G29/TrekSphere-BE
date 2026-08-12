package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.vendor.VendorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProfileResponse {
    private UUID vendorId;
    private String companyName;
    private String description;
    private String logoUrl;
    private String contactEmail;
    private String contactPhone;
    private String taxCode;
    private String businessLicenseUrl;
    private VendorStatus status;
}
