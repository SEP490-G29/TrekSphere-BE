package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignPorterRequest {
    @NotNull(message = MessageConstant.PORTER_ID_REQUIRED)
    private UUID porterId;
    
    private String note;
}
