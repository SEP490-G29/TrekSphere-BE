package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.vendor.VendorStatus;
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
    private String bankAccount;
    private String bankName;
    private VendorStatus status;
}
