package com.sep.treksphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEquipmentCheckResponse {

    private UUID sessionEquipmentId;
    private UUID tourSessionId;
    private UUID equipmentId;
    private String equipmentName;
    private Integer quantity;
    private Boolean isChecked;
    private UUID checkedById;
    private String checkedByName;
    private String note;
    private LocalDateTime updatedAt;
}
