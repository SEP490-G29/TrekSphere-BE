package com.sep.treksphere.dto.request.logistics;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignCoordinatorRequest {

    @NotNull(message = MessageConstant.COORDINATOR_ID_REQUIRED)
    private UUID coordinatorId;

    @NotNull(message = MessageConstant.IS_LEAD_REQUIRED)
    private Boolean isLead;
}
