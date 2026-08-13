package com.sep.treksphere.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep.treksphere.dto.request.*;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.tour.AttendanceType;
import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import com.sep.treksphere.enums.tracking.TrackingEventStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrackingEventProcessor {

    private final TrackingIngestedEventRepository eventRepository;
    private final TrackingSessionRevisionRepository revisionRepository;
    private final TourSessionRepository tourSessionRepository;
    private final SessionCheckpointLogRepository checkpointLogRepository;
    private final BookingParticipantRepository participantRepository;
    private final TrackingService trackingService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TrackingEventResult process(UUID eventId) {
        TrackingIngestedEvent event = eventRepository.findByIdForUpdate(eventId).orElseThrow();
        if (event.getProcessingStatus() != TrackingEventStatus.RECEIVED) {
            return mapResult(event, "DUPLICATE");
        }

        ensureRevisionRow(event.getTourSessionId());
        TrackingSessionRevision revision = revisionRepository.findByIdForUpdate(event.getTourSessionId())
                .orElseThrow();

        ResourceOutcome outcome = apply(event);
        long resultRevision = revision.getRevision();
        revision.setLastEventId(event.getTrackingIngestedEventId());
        revisionRepository.save(revision);

        event.setProcessingStatus(TrackingEventStatus.ACCEPTED);
        event.setProcessedAt(Instant.now());
        event.setResultRevision(resultRevision);
        event.setResourceType(outcome.resourceType());
        event.setResourceId(outcome.resourceId());
        event.setResultMessage(outcome.message());
        eventRepository.save(event);
        return mapResult(event, outcome.duplicate() ? "DUPLICATE" : "ACCEPTED");
    }

    private ResourceOutcome apply(TrackingIngestedEvent event) {
        JsonNode payload = event.getPayload();
        UUID actorId = event.getActorId();
        UUID sessionId = event.getTourSessionId();
        LocalDateTime occurredAt = LocalDateTime.ofInstant(event.getOccurredAt(), ZoneId.systemDefault());

        return switch (event.getEventType()) {
            case EQUIPMENT_CHECKED -> {
                UUID equipmentId = requiredUuid(payload, "sessionEquipmentId");
                SessionEquipmentCheckRequest request = new SessionEquipmentCheckRequest(
                        requiredBoolean(payload, "isChecked"), optionalText(payload, "note"));
                SessionEquipmentCheckResponse response = trackingService.checkEquipment(actorId, equipmentId, request);
                yield new ResourceOutcome("SESSION_EQUIPMENT", response.getSessionEquipmentId(),
                        "Đã đồng bộ kiểm tra thiết bị");
            }
            case ATTENDANCE_START_RECORDED, ATTENDANCE_END_RECORDED -> {
                AttendanceType type = event.getEventType().name().contains("START")
                        ? AttendanceType.START : AttendanceType.END;
                List<ParticipantAttendanceItem> items = attendanceItems(payload);
                TourSessionAttendanceRequest request = new TourSessionAttendanceRequest(type, items);
                trackingService.recordAttendance(actorId, sessionId, request);
                for (ParticipantAttendanceItem item : items) {
                    BookingParticipant participant = participantRepository.findById(item.getParticipantId())
                            .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_FOUND_IN_SESSION));
                    if (type == AttendanceType.START) participant.setStartAttendedAt(occurredAt);
                    else participant.setEndAttendedAt(occurredAt);
                }
                yield new ResourceOutcome("TOUR_SESSION", sessionId, "Đã đồng bộ điểm danh " + type);
            }
            case SESSION_STARTED -> {
                TourSessionStartResponse response = trackingService.startSession(actorId, sessionId);
                TourSession session = tourSessionRepository.findById(sessionId).orElseThrow();
                session.setStartedAt(occurredAt);
                yield new ResourceOutcome("TOUR_SESSION", response.getTourSessionId(), "Đã bắt đầu tour");
            }
            case CHECKPOINT_REACHED -> {
                UUID checkpointId = requiredUuid(payload, "checkpointId");
                SessionCheckpointLog target = checkpointLogRepository
                        .findBySessionAndCheckpointForUpdate(sessionId, checkpointId)
                        .orElseThrow(() -> new AppException(ErrorCode.CHECKPOINT_NOT_FOUND));
                if (target.getStatus() == SessionCheckpointLogStatus.REACHED) {
                    yield new ResourceOutcome("SESSION_CHECKPOINT_LOG",
                            target.getSessionCheckpointLogId(), "Checkpoint đã được ghi nhận trước đó", true);
                }
                List<SessionCheckpointLog> pending = checkpointLogRepository
                        .findByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCheckpoint_CheckpointOrderAsc(
                                sessionId, SessionCheckpointLogStatus.PENDING);
                if (pending.isEmpty() || !pending.getFirst().getCheckpoint().getCheckpointId().equals(checkpointId)) {
                    throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT,
                            "Checkpoint không phải mốc tiếp theo của hành trình");
                }
                SessionCheckpointLogResponse response = trackingService.checkinCheckpoint(
                        actorId, sessionId, checkpointRequest(payload));
                SessionCheckpointLog saved = checkpointLogRepository.findById(response.getSessionCheckpointLogId())
                        .orElseThrow();
                saved.setReachedAt(occurredAt);
                yield new ResourceOutcome("SESSION_CHECKPOINT_LOG", response.getSessionCheckpointLogId(),
                        "Đã ghi nhận checkpoint");
            }
            case CHECKPOINT_SKIPPED -> {
                UUID checkpointId = requiredUuid(payload, "checkpointId");
                SessionCheckpointLog target = checkpointLogRepository
                        .findBySessionAndCheckpointForUpdate(sessionId, checkpointId)
                        .orElseThrow(() -> new AppException(ErrorCode.CHECKPOINT_NOT_FOUND));
                if (target.getStatus() == SessionCheckpointLogStatus.SKIPPED) {
                    yield new ResourceOutcome("SESSION_CHECKPOINT_LOG",
                            target.getSessionCheckpointLogId(), "Checkpoint đã được bỏ qua trước đó", true);
                }
                if (target.getStatus() != SessionCheckpointLogStatus.PENDING) {
                    throw new AppException(ErrorCode.CHECKPOINT_ALREADY_PROCESSED);
                }
                SessionCheckpointLogResponse response = trackingService.skipCheckpoint(
                        actorId,
                        sessionId,
                        checkpointId,
                        new SkipCheckpointRequest(requiredText(payload, "reason")));
                yield new ResourceOutcome("SESSION_CHECKPOINT_LOG", response.getSessionCheckpointLogId(),
                        "Đã bỏ qua checkpoint");
            }
            case SOS_CREATED -> {
                CreateSosAlertRequest request = new CreateSosAlertRequest(
                        sessionId,
                        requiredDecimal(payload, "latitude"),
                        requiredDecimal(payload, "longitude"),
                        optionalText(payload, "message"));
                SosAlertResponse response = trackingService.createSosAlert(actorId, request);
                yield new ResourceOutcome("SOS_ALERT", response.getSosAlertId(), "Đã gửi SOS");
            }
            case SESSION_ENDED -> {
                TourSessionEndResponse response = trackingService.endSession(actorId, sessionId);
                TourSession session = tourSessionRepository.findById(sessionId).orElseThrow();
                session.setEndedAt(occurredAt);
                yield new ResourceOutcome("TOUR_SESSION", response.getTourSessionId(), "Đã kết thúc tour");
            }
            case SOS_RESOLVED -> {
                UUID sosId = requiredUuid(payload, "sosAlertId");
                SosAlertResponse response = trackingService.resolveSosAlert(actorId, sosId);
                yield new ResourceOutcome("SOS_ALERT", response.getSosAlertId(), "Đã giải quyết SOS");
            }
        };
    }

    private SessionCheckpointLogRequest checkpointRequest(JsonNode payload) {
        return new SessionCheckpointLogRequest(
                requiredDecimal(payload, "latitude"),
                requiredDecimal(payload, "longitude"),
                optionalText(payload, "note"));
    }

    private List<ParticipantAttendanceItem> attendanceItems(JsonNode payload) {
        JsonNode participants = payload.get("participants");
        if (participants == null || !participants.isArray() || participants.isEmpty()) {
            throw new IllegalArgumentException("participants là bắt buộc");
        }
        List<ParticipantAttendanceItem> result = new ArrayList<>();
        participants.forEach(item -> result.add(new ParticipantAttendanceItem(
                requiredUuid(item, "participantId"), requiredBoolean(item, "isPresent"))));
        return result;
    }

    private UUID requiredUuid(JsonNode payload, String field) {
        String value = requiredText(payload, field);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(field + " không phải UUID hợp lệ");
        }
    }

    private BigDecimal requiredDecimal(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isNumber()) throw new IllegalArgumentException(field + " là bắt buộc");
        return value.decimalValue();
    }

    private Boolean requiredBoolean(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isBoolean()) throw new IllegalArgumentException(field + " là bắt buộc");
        return value.booleanValue();
    }

    private String requiredText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " là bắt buộc");
        }
        return value.asText();
    }

    private String optionalText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private void ensureRevisionRow(UUID sessionId) {
        jdbcTemplate.update("""
                INSERT INTO tracking_session_revision(tour_session_id, revision, updated_at)
                VALUES (?, 0, CURRENT_TIMESTAMP)
                ON CONFLICT (tour_session_id) DO NOTHING
                """, sessionId);
    }

    private TrackingEventResult mapResult(TrackingIngestedEvent event, String status) {
        return TrackingEventResult.builder()
                .clientEventId(event.getClientEventId())
                .sequenceNumber(event.getSequenceNumber())
                .status(status)
                .code(event.getErrorCode())
                .message(event.getResultMessage())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .resultRevision(event.getResultRevision())
                .build();
    }

    private record ResourceOutcome(String resourceType, UUID resourceId, String message, boolean duplicate) {
        private ResourceOutcome(String resourceType, UUID resourceId, String message) {
            this(resourceType, resourceId, message, false);
        }
    }
}
