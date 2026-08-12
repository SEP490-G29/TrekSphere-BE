package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayOsAccountConfigRequest {
    @NotBlank
    private String clientId;

    @NotBlank
    private String apiKey;

    @NotBlank
    private String checksumKey;
}
