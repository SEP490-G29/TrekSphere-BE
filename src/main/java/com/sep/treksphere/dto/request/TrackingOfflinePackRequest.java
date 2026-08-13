package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TrackingOfflinePackRequest {
    @NotNull
    private UUID deviceId;
}
