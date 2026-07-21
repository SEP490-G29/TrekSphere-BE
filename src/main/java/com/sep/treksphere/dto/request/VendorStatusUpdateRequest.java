package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.vendor.VendorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorStatusUpdateRequest {

    @NotNull(message = MessageConstant.VENDOR_STATUS_REQUIRED)
    private VendorStatus status;
}
