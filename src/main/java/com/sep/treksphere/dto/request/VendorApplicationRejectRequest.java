package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorApplicationRejectRequest {

    @NotBlank(message = MessageConstant.REJECTION_REASON_REQUIRED)
    private String rejectionReason;
}
