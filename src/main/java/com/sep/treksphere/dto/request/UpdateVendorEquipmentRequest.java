package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateVendorEquipmentRequest {
    private String equipmentName;

    private String description;

    @Min(value = 0, message = "Số lượng tổng không được nhỏ hơn 0")
    private Integer totalQuantity;
}
