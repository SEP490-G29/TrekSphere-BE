package com.sep.treksphere.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkReturnEquipmentRequest {

    @NotEmpty(message = "Danh sách trang bị hoàn trả không được để trống")
    @Valid
    private List<ReturnEquipmentItemRequest> items;
}
