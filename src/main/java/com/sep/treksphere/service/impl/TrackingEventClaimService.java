package com.sep.treksphere.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.dto.request.TrackingSyncEventRequest;
import com.sep.treksphere.entity.TrackingIngestedEvent;
import com.sep.treksphere.enums.tracking.TrackingEventStatus;
import com.sep.treksphere.repository.TrackingIngestedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingEventClaimService {

    private final JdbcTemplate jdbcTemplate;
    private final TrackingIngestedEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimResult claim(UUID actorId, UUID sessionId, UUID deviceId, TrackingSyncEventRequest request) {
        String payloadJson = toJson(request);
        String payloadHash = sha256(request.getType().name()
                + "|" + request.getSequenceNumber()
                + "|" + request.getOccurredAt()
                + "|" + request.getBaseRevision()
                + "|" + payloadJson);
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        int inserted = jdbcTemplate.update("""
                INSERT INTO tracking_ingested_event(
                    tracking_ingested_event_id, client_event_id, tour_session_id, actor_id,
                    device_id, sequence_number, event_type, occurred_at, received_at,
                    payload, payload_hash, base_revision, processing_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, 'RECEIVED')
                ON CONFLICT DO NOTHING
                """,
                eventId, request.getClientEventId(), sessionId, actorId, deviceId,
                request.getSequenceNumber(), request.getType().name(), request.getOccurredAt(), now,
                request.getPayload().toString(), payloadHash, request.getBaseRevision());

        if (inserted == 1) {
            return new ClaimResult(eventRepository.findById(eventId).orElseThrow(), true, null);
        }

        TrackingIngestedEvent existing = eventRepository
                .findByActorIdAndDeviceIdAndClientEventId(actorId, deviceId, request.getClientEventId())
                .orElse(null);
        if (existing == null) {
            existing = eventRepository
                    .findByTourSessionIdAndActorIdAndDeviceIdAndSequenceNumber(
                            sessionId, actorId, deviceId, request.getSequenceNumber())
                    .orElse(null);
            return new ClaimResult(existing, false, "SEQUENCE_NUMBER_REUSED");
        }
        if (!existing.getPayloadHash().equals(payloadHash)
                || existing.getEventType() != request.getType()
                || !existing.getTourSessionId().equals(sessionId)) {
            return new ClaimResult(existing, false, "IDEMPOTENCY_KEY_REUSED");
        }
        return new ClaimResult(existing, false, null);
    }

    private String toJson(TrackingSyncEventRequest request) {
        try {
            return objectMapper.writeValueAsString(request.getPayload());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Payload không thể chuyển thành JSON", ex);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public record ClaimResult(TrackingIngestedEvent event, boolean newlyCreated, String conflictCode) {
        public boolean completed() {
            return event != null && event.getProcessingStatus() != TrackingEventStatus.RECEIVED;
        }
    }
}
