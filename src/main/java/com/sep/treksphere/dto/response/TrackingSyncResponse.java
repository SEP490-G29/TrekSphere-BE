package com.sep.treksphere.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TrackingSyncResponse {
    private UUID sessionId;
    private Long revision;
    private Instant serverTime;
    private List<TrackingEventResult> results;
    private TrackingSnapshotResponse snapshot;
}
