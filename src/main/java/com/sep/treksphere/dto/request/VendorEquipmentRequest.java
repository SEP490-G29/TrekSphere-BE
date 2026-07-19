package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorEquipmentRequest {
    @NotBlank(message = "Tên trang bị không được để trống")
    private String equipmentName;

    private String description;

    @Min(value = 0, message = "Số lượng tổng không được nhỏ hơn 0")
    private Integer totalQuantity = 0;
}
