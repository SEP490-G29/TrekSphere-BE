package com.sep.treksphere.vendor;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.vendor.VendorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorFilterRequest extends BaseFilterRequest {

    @Schema(
            description = "Lọc theo trạng thái nhà cung cấp",
            allowableValues = {"ACTIVE", "INACTIVE", "REVOKED"}
    )
    private VendorStatus status;
}
