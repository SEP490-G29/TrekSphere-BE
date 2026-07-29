package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEquipmentCheckRequest {

    @NotNull(message = MessageConstant.EQUIPMENT_CHECK_STATUS_REQUIRED)
    private Boolean isChecked;

    private String note;
}
