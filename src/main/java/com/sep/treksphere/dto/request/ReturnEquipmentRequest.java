package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnEquipmentRequest {

    @NotNull(message = MessageConstant.RETURN_QUANTITY_REQUIRED)
    @Min(value = 0, message = MessageConstant.QUANTITY_MIN_ZERO)
    private Integer returnedQuantity;

    @NotNull(message = MessageConstant.MISSING_QUANTITY_REQUIRED)
    @Min(value = 0, message = MessageConstant.QUANTITY_MIN_ZERO)
    private Integer missingQuantity;

    private String note;
}
