package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class TrackingLocationSampleRequest {
    @NotNull
    private UUID sampleId;
    @NotNull
    private Instant recordedAt;
    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal latitude;
    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal longitude;
    @DecimalMin("0")
    private BigDecimal accuracyMeters;
    @DecimalMin("0")
    private BigDecimal speedMetersPerSecond;
    @DecimalMin("0")
    @DecimalMax(value = "359.99")
    private BigDecimal headingDegrees;
}
