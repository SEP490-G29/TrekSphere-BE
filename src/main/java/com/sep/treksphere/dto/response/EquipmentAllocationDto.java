package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.logistics.EquipmentReturnStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EquipmentAllocationDto {
    private UUID sessionEquipmentId;
    private UUID equipmentId;
    private String equipmentName;
    private Integer quantity;
    private String note;
    private Boolean isChecked;
    private Integer returnedQuantity;
    private Integer missingQuantity;
    private EquipmentReturnStatus returnStatus;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
}

