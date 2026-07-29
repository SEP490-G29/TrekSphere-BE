package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class AssignEquipmentRequest {
    
    @NotNull(message = MessageConstant.EQUIPMENT_ID_REQUIRED)
    private UUID equipmentId;

    @NotNull(message = MessageConstant.QUANTITY_REQUIRED_AND_MIN)
    @Min(value = 1, message = MessageConstant.QUANTITY_REQUIRED_AND_MIN)
    private Integer quantity;

    private String note;
}
