package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.*;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.tracking.*;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.TrackingOfflineService;
import com.sep.treksphere.service.TrackingSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingOfflineServiceImpl implements TrackingOfflineService {

    private static final int MAX_EVENT_BATCH_SIZE = 100;
    private static final int MAX_LOCATION_BATCH_SIZE = 200;
    private static final int GPS_INTERVAL_SECONDS = 30;

    private final TourSessionRepository tourSessionRepository;
    private final CoordinatorScheduleRepository coordinatorScheduleRepository;
    private final TrackingDeviceSessionRepository deviceSessionRepository;
    private final TrackingIngestedEventRepository eventRepository;
    private final TrackingSnapshotService snapshotService;
    private final TrackingEventClaimService claimService;
    private final TrackingEventProcessor eventProcessor;
    private final TrackingEventResultRecorder resultRecorder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public TrackingOfflinePackResponse createOfflinePack(
            UUID actorId,
            UUID sessionId,
            TrackingOfflinePackRequest request
    ) {
        TourSession session = getSession(sessionId);
        CoordinatorSchedule assignment = getActiveAssignment(sessionId, actorId);
        Instant now = Instant.now();
        Instant expiresAt = session.getTourSchedule().getReturnDate().plusDays(2)
                .atStartOfDay(ZoneId.systemDefault()).toInstant();

        TrackingDeviceSession deviceSession = deviceSessionRepository
                .findByTourSession_TourSessionIdAndActor_UserIdAndDeviceId(sessionId, actorId, request.getDeviceId())
                .orElseGet(TrackingDeviceSession::new);
        if (deviceSession.getTrackingDeviceSessionId() == null) {
            deviceSession.setTrackingDeviceSessionId(UUID.randomUUID());
            deviceSession.setTourSession(session);
            deviceSession.setActor(assignment.getCoordinator());
            deviceSession.setDeviceId(request.getDeviceId());
            deviceSession.setIssuedAt(now);
        }
        deviceSession.setStatus(TrackingDeviceSessionStatus.ACTIVE);
        deviceSession.setExpiresAt(expiresAt);
        deviceSession.setLastSeenAt(now);
        deviceSession.setRevokedAt(null);
        deviceSessionRepository.save(deviceSession);
        ensureRevisionRow(sessionId);

        return TrackingOfflinePackResponse.builder()
                .deviceSessionId(deviceSession.getTrackingDeviceSessionId())
                .deviceId(deviceSession.getDeviceId())
                .actorId(actorId)
                .leadCoordinator(assignment.getIsLead())
                .issuedAt(deviceSession.getIssuedAt())
                .expiresAt(expiresAt)
                .serverTime(now)
                .maxEventBatchSize(MAX_EVENT_BATCH_SIZE)
                .maxLocationBatchSize(MAX_LOCATION_BATCH_SIZE)
                .gpsIntervalSeconds(GPS_INTERVAL_SECONDS)
                .snapshot(snapshotService.getSnapshot(sessionId))
                .build();
    }

    @Override
    public TrackingSyncResponse sync(UUID actorId, UUID sessionId, TrackingSyncRequest request) {
        validateDeviceSession(actorId, sessionId, request.getDeviceSessionId(), request.getDeviceId());
        List<TrackingSyncEventRequest> events = request.getEvents().stream()
                .sorted(Comparator.comparing(TrackingSyncEventRequest::getSequenceNumber))
                .toList();
        ensureNoDuplicateIdsOrSequences(events);

        List<TrackingEventResult> results = new ArrayList<>();
        for (TrackingSyncEventRequest eventRequest : events) {
            TrackingEventClaimService.ClaimResult claim;
            try {
                claim = claimService.claim(actorId, sessionId, request.getDeviceId(), eventRequest);
            } catch (RuntimeException ex) {
                log.error("Cannot claim tracking event {}", eventRequest.getClientEventId(), ex);
                results.add(retryable(eventRequest));
                continue;
            }

            if (claim.conflictCode() != null) {
                results.add(TrackingEventResult.builder()
                        .clientEventId(eventRequest.getClientEventId())
                        .sequenceNumber(eventRequest.getSequenceNumber())
                        .status("CONFLICT")
                        .code(claim.conflictCode())
                        .message("Event ID hoặc sequence đã được dùng cho dữ liệu khác")
                        .build());
                continue;
            }

            if (claim.completed()) {
                String status = claim.event().getProcessingStatus() == TrackingEventStatus.ACCEPTED
                        ? "DUPLICATE" : claim.event().getProcessingStatus().name();
                results.add(resultRecorder.toResult(claim.event(), status));
                continue;
            }

            try {
                validateOccurredAt(getSession(sessionId), eventRequest.getOccurredAt());
                results.add(eventProcessor.process(claim.event().getTrackingIngestedEventId()));
            } catch (AppException | IllegalArgumentException ex) {
                results.add(resultRecorder.recordFailure(claim.event().getTrackingIngestedEventId(), ex));
            } catch (RuntimeException ex) {
                log.error("Retryable error while processing tracking event {}",
                        eventRequest.getClientEventId(), ex);
                results.add(retryable(eventRequest));
            }
        }

        touchDeviceSession(request.getDeviceSessionId());
        TrackingSnapshotResponse snapshot = snapshotService.getSnapshot(sessionId);
        return TrackingSyncResponse.builder()
                .sessionId(sessionId)
                .revision(snapshot.getRevision())
                .serverTime(Instant.now())
                .results(results)
                .snapshot(snapshot)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingSyncStateResponse getSyncState(UUID actorId, UUID sessionId, long afterRevision) {
        getActiveAssignment(sessionId, actorId);
        TrackingSnapshotResponse snapshot = snapshotService.getSnapshot(sessionId);
        List<TrackingEventResult> changes = eventRepository
                .findTop100ByTourSessionIdAndProcessingStatusAndResultRevisionGreaterThanOrderByResultRevisionAsc(
                        sessionId, TrackingEventStatus.ACCEPTED, afterRevision)
                .stream()
                .map(event -> resultRecorder.toResult(event, "ACCEPTED"))
                .toList();
        return TrackingSyncStateResponse.builder()
                .revision(snapshot.getRevision())
                .fullSnapshot(true)
                .changes(changes)
                .snapshot(snapshot)
                .build();
    }

    private void validateDeviceSession(UUID actorId, UUID sessionId, UUID deviceSessionId, UUID deviceId) {
        TrackingDeviceSession deviceSession = deviceSessionRepository.findDetailedById(deviceSessionId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS,
                        "Thiết bị chưa đăng ký offline pack"));
        boolean valid = deviceSession.getTourSession().getTourSessionId().equals(sessionId)
                && deviceSession.getActor().getUserId().equals(actorId)
                && deviceSession.getDeviceId().equals(deviceId)
                && deviceSession.getStatus() == TrackingDeviceSessionStatus.ACTIVE
                && deviceSession.getExpiresAt().isAfter(Instant.now());
        if (!valid) {
            throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS,
                    "Phiên đồng bộ của thiết bị không hợp lệ hoặc đã hết hạn");
        }
        getActiveAssignment(sessionId, actorId);
    }

    private CoordinatorSchedule getActiveAssignment(UUID sessionId, UUID actorId) {
        CoordinatorSchedule assignment = coordinatorScheduleRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, actorId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS));
        if (Boolean.TRUE.equals(assignment.getIsCancelled())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
        }
        return assignment;
    }

    private TourSession getSession(UUID sessionId) {
        return tourSessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void validateOccurredAt(TourSession session, Instant occurredAt) {
        Instant now = Instant.now();
        if (occurredAt.isAfter(now.plus(Duration.ofMinutes(5)))) {
            throw new IllegalArgumentException("occurredAt vượt quá thời gian server cho phép");
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant earliest = session.getTourSchedule().getDepartureDate().minusDays(1).atStartOfDay(zone).toInstant();
        Instant latest = session.getTourSchedule().getReturnDate().plusDays(2).atStartOfDay(zone).toInstant();
        if (occurredAt.isBefore(earliest) || occurredAt.isAfter(latest)) {
            throw new IllegalArgumentException("occurredAt nằm ngoài thời gian của chuyến đi");
        }
    }

    private void ensureNoDuplicateIdsOrSequences(List<TrackingSyncEventRequest> events) {
        Set<UUID> ids = new HashSet<>();
        Set<Long> sequences = new HashSet<>();
        for (TrackingSyncEventRequest event : events) {
            if (!ids.add(event.getClientEventId()) || !sequences.add(event.getSequenceNumber())) {
                throw new IllegalArgumentException("Batch chứa clientEventId hoặc sequenceNumber trùng");
            }
        }
    }

    private TrackingEventResult retryable(TrackingSyncEventRequest request) {
        return TrackingEventResult.builder()
                .clientEventId(request.getClientEventId())
                .sequenceNumber(request.getSequenceNumber())
                .status("RETRYABLE")
                .code("TEMPORARY_PROCESSING_ERROR")
                .message("Lỗi tạm thời, vui lòng gửi lại event")
                .build();
    }

    private void ensureRevisionRow(UUID sessionId) {
        jdbcTemplate.update("""
                INSERT INTO tracking_session_revision(tour_session_id, revision, updated_at)
                VALUES (?, 0, CURRENT_TIMESTAMP)
                ON CONFLICT (tour_session_id) DO NOTHING
                """, sessionId);
    }

    @Transactional
    protected void touchDeviceSession(UUID id) {
        deviceSessionRepository.findById(id).ifPresent(deviceSession -> {
            deviceSession.setLastSeenAt(Instant.now());
            deviceSessionRepository.save(deviceSession);
        });
    }
}
