package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualRefundCompletionRequest {
    @NotBlank
    private String bankReference;

    private String note;
}
