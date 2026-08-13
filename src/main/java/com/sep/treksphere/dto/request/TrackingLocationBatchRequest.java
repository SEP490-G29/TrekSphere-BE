package com.sep.treksphere.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class TrackingLocationBatchRequest {
    @NotNull
    private UUID deviceSessionId;
    @NotNull
    private UUID deviceId;
    @Valid
    @NotEmpty
    @Size(max = 200)
    private List<TrackingLocationSampleRequest> samples;
}
