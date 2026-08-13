package com.sep.treksphere.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep.treksphere.enums.tracking.TrackingEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class TrackingSyncEventRequest {
    @NotNull
    private UUID clientEventId;
    @NotNull
    @PositiveOrZero
    private Long sequenceNumber;
    @NotNull
    private TrackingEventType type;
    @NotNull
    private Instant occurredAt;
    @PositiveOrZero
    private Long baseRevision;
    @NotNull
    private JsonNode payload;
}
