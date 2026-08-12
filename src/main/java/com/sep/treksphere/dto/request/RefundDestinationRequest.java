package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundDestinationRequest {
    @NotBlank
    private String bankBin;
    @NotBlank
    private String accountNumber;
    @NotBlank
    private String accountName;
}
