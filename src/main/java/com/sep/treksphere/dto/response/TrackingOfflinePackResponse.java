package com.sep.treksphere.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TrackingOfflinePackResponse {
    private UUID deviceSessionId;
    private UUID deviceId;
    private UUID actorId;
    private Boolean leadCoordinator;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant serverTime;
    private Integer maxEventBatchSize;
    private Integer maxLocationBatchSize;
    private Integer gpsIntervalSeconds;
    private TrackingSnapshotResponse snapshot;
}
