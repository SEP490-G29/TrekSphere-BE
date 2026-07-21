package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.vendor.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorApplicationReviewRequest {

    @NotNull(message = MessageConstant.REVIEW_STATUS_REQUIRED)
    private ApplicationStatus status;

    private String rejectionReason;
}
