package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.*;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import com.sep.treksphere.enums.tracking.*;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.TrackingLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrackingLocationServiceImpl implements TrackingLocationService {

    private static final BigDecimal LOW_ACCURACY_METERS = BigDecimal.valueOf(100);
    private static final Duration STALE_AFTER = Duration.ofMinutes(2);

    private final TrackingDeviceSessionRepository deviceSessionRepository;
    private final TrackingLocationSampleRepository locationRepository;
    private final TourSessionRepository sessionRepository;
    private final CoordinatorScheduleRepository coordinatorRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public TrackingLocationBatchResponse ingest(
            UUID actorId,
            UUID sessionId,
            TrackingLocationBatchRequest request
    ) {
        TourSession session = sessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        if (session.getStatus() != TourSessionStatus.IN_PROGRESS
                && session.getStatus() != TourSessionStatus.COMPLETED) {
            throw new AppException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        }
        validateDevice(actorId, sessionId, request.getDeviceSessionId(), request.getDeviceId());

        List<UUID> accepted = new ArrayList<>();
        List<UUID> duplicates = new ArrayList<>();
        List<TrackingLocationBatchResponse.RejectedSample> rejected = new ArrayList<>();
        Set<UUID> requestIds = new HashSet<>();
        Instant receivedAt = Instant.now();

        for (TrackingLocationSampleRequest sample : request.getSamples()) {
            if (!requestIds.add(sample.getSampleId()) || locationRepository.existsById(sample.getSampleId())) {
                duplicates.add(sample.getSampleId());
                continue;
            }
            String error = validateSample(session, sample, receivedAt);
            if (error != null) {
                rejected.add(TrackingLocationBatchResponse.RejectedSample.builder()
                        .sampleId(sample.getSampleId())
                        .code("INVALID_LOCATION_SAMPLE")
                        .message(error)
                        .build());
                continue;
            }

            TrackingLocationValidationStatus status = sample.getAccuracyMeters() != null
                    && sample.getAccuracyMeters().compareTo(LOW_ACCURACY_METERS) > 0
                    ? TrackingLocationValidationStatus.LOW_ACCURACY
                    : TrackingLocationValidationStatus.VALID;
            boolean late = sample.getRecordedAt().isBefore(receivedAt.minus(Duration.ofMinutes(2)));
            int inserted = jdbcTemplate.update("""
                    INSERT INTO tracking_location_sample(
                        sample_id, tour_session_id, actor_id, device_id, recorded_at, received_at,
                        latitude, longitude, accuracy_meters, speed_mps, heading_degrees,
                        validation_status, is_late
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (sample_id) DO NOTHING
                    """, sample.getSampleId(), sessionId, actorId, request.getDeviceId(),
                    Timestamp.from(sample.getRecordedAt()), Timestamp.from(receivedAt),
                    sample.getLatitude(), sample.getLongitude(),
                    sample.getAccuracyMeters(), sample.getSpeedMetersPerSecond(), sample.getHeadingDegrees(),
                    status.name(), late);
            if (inserted == 1) accepted.add(sample.getSampleId());
            else duplicates.add(sample.getSampleId());
        }

        if (!accepted.isEmpty()) {
            List<TrackingLocationResponse> updates = locationRepository.findAllById(accepted)
                    .stream().map(this::mapLocation).toList();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(
                            "/topic/tracking/sessions/" + sessionId + "/locations", updates);
                }
            });
        }

        return TrackingLocationBatchResponse.builder()
                .acceptedSampleIds(accepted)
                .duplicateSampleIds(duplicates)
                .rejectedSamples(rejected)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingLocationResponse> getLatest(UUID viewerId, UUID sessionId) {
        authorizeViewer(viewerId, sessionId);
        return locationRepository.findLatestBySessionId(sessionId).stream().map(this::mapLocation).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingLocationResponse> getHistory(
            UUID viewerId,
            UUID sessionId,
            UUID actorId,
            Instant from,
            Instant to,
            int limit
    ) {
        authorizeViewer(viewerId, sessionId);
        if (from.isAfter(to)) throw new IllegalArgumentException("from phải trước to");
        PageRequest page = PageRequest.of(0, Math.min(Math.max(limit, 1), 5000));
        List<TrackingLocationSample> samples = actorId == null
                ? locationRepository.findByTourSessionIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                        sessionId, from, to, page)
                : locationRepository.findByTourSessionIdAndActorIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                        sessionId, actorId, from, to, page);
        return samples.stream().map(this::mapLocation).toList();
    }

    private void validateDevice(UUID actorId, UUID sessionId, UUID deviceSessionId, UUID deviceId) {
        TrackingDeviceSession device = deviceSessionRepository.findDetailedById(deviceSessionId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS));
        boolean valid = device.getTourSession().getTourSessionId().equals(sessionId)
                && device.getActor().getUserId().equals(actorId)
                && device.getDeviceId().equals(deviceId)
                && device.getStatus() == TrackingDeviceSessionStatus.ACTIVE
                && device.getExpiresAt().isAfter(Instant.now());
        if (!valid) throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
        CoordinatorSchedule assignment = coordinatorRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, actorId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS));
        if (Boolean.TRUE.equals(assignment.getIsCancelled())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
        }
    }

    private String validateSample(TourSession session, TrackingLocationSampleRequest sample, Instant now) {
        if (sample.getRecordedAt().isAfter(now.plus(Duration.ofMinutes(5)))) {
            return "recordedAt vượt quá thời gian server cho phép";
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant earliest = session.getTourSchedule().getDepartureDate().minusDays(1).atStartOfDay(zone).toInstant();
        Instant latest = session.getTourSchedule().getReturnDate().plusDays(2).atStartOfDay(zone).toInstant();
        if (sample.getRecordedAt().isBefore(earliest) || sample.getRecordedAt().isAfter(latest)) {
            return "recordedAt nằm ngoài thời gian chuyến đi";
        }
        if (session.getStartedAt() != null) {
            Instant startedAt = session.getStartedAt().atZone(zone).toInstant();
            if (sample.getRecordedAt().isBefore(startedAt.minus(Duration.ofMinutes(5)))) {
                return "recordedAt xảy ra trước khi tour bắt đầu";
            }
        }
        if (session.getEndedAt() != null) {
            Instant endedAt = session.getEndedAt().atZone(zone).toInstant();
            if (sample.getRecordedAt().isAfter(endedAt.plus(Duration.ofMinutes(5)))) {
                return "recordedAt xảy ra sau khi tour kết thúc";
            }
        }
        return null;
    }

    private void authorizeViewer(UUID viewerId, UUID sessionId) {
        TourSession session = sessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        Optional<CoordinatorSchedule> coordinator = coordinatorRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, viewerId);
        if (coordinator.filter(item -> !Boolean.TRUE.equals(item.getIsCancelled())).isPresent()) return;

        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UUID sessionVendorId = session.getTourSchedule().getTour().getVendor().getVendorId();
        boolean manager = viewer.getRoles().stream().anyMatch(role -> "VENDOR_MANAGER".equals(role.getRoleName()))
                && vendorRepository.findByManager_UserId(viewerId)
                .map(vendor -> vendor.getVendorId().equals(sessionVendorId)).orElse(false);
        boolean staff = viewer.getRoles().stream().anyMatch(role -> "VENDOR_STAFF".equals(role.getRoleName()))
                && vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(viewerId)
                .map(item -> item.getVendor().getVendorId().equals(sessionVendorId)).orElse(false);
        if (!manager && !staff) throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
    }

    private TrackingLocationResponse mapLocation(TrackingLocationSample sample) {
        return TrackingLocationResponse.builder()
                .sampleId(sample.getSampleId())
                .sessionId(sample.getTourSessionId())
                .actorId(sample.getActorId())
                .deviceId(sample.getDeviceId())
                .recordedAt(sample.getRecordedAt())
                .receivedAt(sample.getReceivedAt())
                .latitude(sample.getLatitude())
                .longitude(sample.getLongitude())
                .accuracyMeters(sample.getAccuracyMeters())
                .speedMetersPerSecond(sample.getSpeedMps())
                .headingDegrees(sample.getHeadingDegrees())
                .validationStatus(sample.getValidationStatus())
                .late(sample.getIsLate())
                .stale(sample.getRecordedAt().isBefore(Instant.now().minus(STALE_AFTER)))
                .build();
    }
}
