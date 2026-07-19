package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorStaffStatusUpdateRequest {

    @NotNull(message = MessageConstant.STAFF_STATUS_REQUIRED)
    private Boolean isActive;
}
