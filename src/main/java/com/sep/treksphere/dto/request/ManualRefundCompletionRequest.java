package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualRefundCompletionRequest {
    @NotBlank
    @Size(max = 500)
    private String receiptImageUrl;

    @Size(max = 300)
    private String note;
}
