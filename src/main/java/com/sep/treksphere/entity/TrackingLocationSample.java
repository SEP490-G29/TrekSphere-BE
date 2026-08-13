package com.sep.treksphere.entity;

import com.sep.treksphere.enums.tracking.TrackingLocationValidationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracking_location_sample")
@Getter
@Setter
@NoArgsConstructor
public class TrackingLocationSample {

    @Id
    private UUID sampleId;

    @Column(nullable = false)
    private UUID tourSessionId;

    @Column(nullable = false)
    private UUID actorId;

    @Column(nullable = false)
    private UUID deviceId;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 8, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(precision = 8, scale = 2)
    private BigDecimal speedMps;

    @Column(precision = 6, scale = 2)
    private BigDecimal headingDegrees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrackingLocationValidationStatus validationStatus;

    @Column(nullable = false)
    private Boolean isLate = false;
}
