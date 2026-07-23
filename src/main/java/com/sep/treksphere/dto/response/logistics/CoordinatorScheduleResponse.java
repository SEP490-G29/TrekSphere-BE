package com.sep.treksphere.dto.response.logistics;

import com.sep.treksphere.dto.response.UserResponse;
import lombok.Data;

import java.util.UUID;

@Data
public class CoordinatorScheduleResponse {
    private UUID coordinatorScheduleId;
    private UserResponse coordinator;
    private Boolean isLead;
}
