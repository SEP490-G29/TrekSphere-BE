package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorEquipmentRequest {
    @NotBlank(message = MessageConstant.EQUIPMENT_NAME_REQUIRED)
    private String equipmentName;

    private String description;

    @Min(value = 0, message = MessageConstant.EQUIPMENT_QUANTITY_MIN)
    private Integer totalQuantity = 0;
}
