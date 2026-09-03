package com.sep.treksphere.vendor.application.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.vendor.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorApplicationReviewRequest {

    @NotNull(message = MessageConstant.REVIEW_STATUS_REQUIRED)
    private ApplicationStatus status;

    private String rejectionReason;
}
