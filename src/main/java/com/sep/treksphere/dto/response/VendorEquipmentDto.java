package com.sep.treksphere.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VendorEquipmentDto {
    private UUID equipmentId;
    private UUID vendorId;
    private String equipmentName;
    private String description;
    private Integer totalQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
