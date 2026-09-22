package com.sep.treksphere.vendor;

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
public class PublicVendorProfileResponse {
    private UUID vendorId;
    private String companyName;
    private String description;
    private String logoUrl;
    private String businessAddress;
    private String websiteUrl;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime partnerSince;
    private long publishedTourCount;
}
