package com.sep.treksphere.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class PorterAllocationDto {
    private UUID porterScheduleId;
    private UUID porterId;
    private String fullName;
    private String phone;
    private String note;
}
