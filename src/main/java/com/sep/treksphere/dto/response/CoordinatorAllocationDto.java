package com.sep.treksphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoordinatorAllocationDto {
    private UUID coordinatorScheduleId;
    private UUID coordinatorId;
    private String fullName;
    private String phone;
    private String email;
    private String avatar;
    private Boolean isLead;
}
