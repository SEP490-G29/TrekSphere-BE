package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tracking.TrackingLocationValidationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TrackingLocationResponse {
    private UUID sampleId;
    private UUID sessionId;
    private UUID actorId;
    private UUID deviceId;
    private Instant recordedAt;
    private Instant receivedAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
    private BigDecimal speedMetersPerSecond;
    private BigDecimal headingDegrees;
    private TrackingLocationValidationStatus validationStatus;
    private Boolean late;
    private Boolean stale;
}
