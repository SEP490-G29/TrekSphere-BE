package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HideTourRequest {

    @NotBlank(message = MessageConstant.HIDE_REASON_REQUIRED)
    private String reason;
}
