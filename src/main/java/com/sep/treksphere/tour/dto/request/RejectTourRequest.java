package com.sep.treksphere.tour.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectTourRequest {

    @NotBlank(message = MessageConstant.REJECTION_REASON_REQUIRED)
    private String reason;
}
