package com.sep.treksphere.tour.schedule;

import com.sep.treksphere.common.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelScheduleRequest {
    @NotBlank(message = MessageConstant.SCHEDULE_CANCEL_REASON_REQUIRED)
    private String reason;
}
