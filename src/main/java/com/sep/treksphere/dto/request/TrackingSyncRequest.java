package com.sep.treksphere.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class TrackingSyncRequest {
    @NotNull
    private UUID deviceSessionId;
    @NotNull
    private UUID deviceId;
    private Long lastKnownRevision;
    @Valid
    @NotEmpty
    @Size(max = 100)
    private List<TrackingSyncEventRequest> events;
}
