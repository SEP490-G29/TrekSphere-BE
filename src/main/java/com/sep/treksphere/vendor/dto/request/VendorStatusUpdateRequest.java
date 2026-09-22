package com.sep.treksphere.vendor.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.vendor.enums.VendorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorStatusUpdateRequest {

    @NotNull(message = MessageConstant.VENDOR_STATUS_REQUIRED)
    private VendorStatus status;
}
