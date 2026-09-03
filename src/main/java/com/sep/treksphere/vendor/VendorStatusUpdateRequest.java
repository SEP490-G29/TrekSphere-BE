package com.sep.treksphere.vendor;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.vendor.VendorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorStatusUpdateRequest {

    @NotNull(message = MessageConstant.VENDOR_STATUS_REQUIRED)
    private VendorStatus status;
}
