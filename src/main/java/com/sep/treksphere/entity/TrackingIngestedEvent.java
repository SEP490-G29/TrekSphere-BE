package com.sep.treksphere.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep.treksphere.enums.tracking.TrackingEventStatus;
import com.sep.treksphere.enums.tracking.TrackingEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracking_ingested_event")
@Getter
@Setter
@NoArgsConstructor
public class TrackingIngestedEvent {

    @Id
    private UUID trackingIngestedEventId;

    @Column(nullable = false)
    private UUID clientEventId;

    @Column(nullable = false)
    private UUID tourSessionId;

    @Column(nullable = false)
    private UUID actorId;

    @Column(nullable = false)
    private UUID deviceId;

    @Column(nullable = false)
    private Long sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TrackingEventType eventType;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant receivedAt;

    private Instant processedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(nullable = false, length = 64)
    private String payloadHash;

    private Long baseRevision;
    private Long resultRevision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrackingEventStatus processingStatus;

    @Column(length = 100)
    private String errorCode;

    @Column(length = 500)
    private String resultMessage;

    @Column(length = 50)
    private String resourceType;

    private UUID resourceId;
}
