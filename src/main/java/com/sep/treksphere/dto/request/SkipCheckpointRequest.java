package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkipCheckpointRequest {

    @NotBlank(message = MessageConstant.CHECKPOINT_SKIP_REASON_REQUIRED)
    @Size(max = 1000, message = MessageConstant.CHECKPOINT_SKIP_REASON_MAX_LENGTH)
    private String reason;
}
