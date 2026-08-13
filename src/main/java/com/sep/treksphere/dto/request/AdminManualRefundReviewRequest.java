package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminManualRefundReviewRequest {
    @NotNull
    private Boolean approved;

    @NotBlank
    @Size(max = 500)
    private String note;
}
