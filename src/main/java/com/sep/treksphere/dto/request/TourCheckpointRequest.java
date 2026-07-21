package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TourCheckpointRequest {

    @NotBlank(message = MessageConstant.CHECKPOINT_NAME_REQUIRED)
    private String checkpointName;

    private String description;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private BigDecimal altitude;

    @NotNull(message = MessageConstant.CHECKPOINT_ORDER_REQUIRED)
    @Min(value = 1, message = MessageConstant.CHECKPOINT_ORDER_POSITIVE)
    private Integer checkpointOrder;

    private String checkpointImageUrl;
}
