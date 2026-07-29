package com.sep.treksphere.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class EquipmentAllocationDto {
    private UUID sessionEquipmentId;
    private UUID equipmentId;
    private String equipmentName;
    private Integer quantity;
    private String note;
}
