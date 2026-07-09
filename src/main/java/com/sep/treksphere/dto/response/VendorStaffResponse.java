package com.sep.treksphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VendorStaffResponse {
    private String vendorStaffId;
    private String vendorId;
    private UserResponse user;
    private Boolean isActive;
    private LocalDateTime deactivatedAt;
}
