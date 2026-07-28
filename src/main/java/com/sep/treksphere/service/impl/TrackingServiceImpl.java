package com.sep.treksphere.service.impl;

import com.sep.treksphere.constant.ValidationConstant;
import com.sep.treksphere.dto.request.SessionCheckpointLogRequest;
import com.sep.treksphere.dto.response.SessionCheckpointLogResponse;
import com.sep.treksphere.dto.response.TourSessionStartResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.TrackingService;
import com.sep.treksphere.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingServiceImpl implements TrackingService {

    private final TourSessionRepository tourSessionRepository;
    private final CoordinatorScheduleRepository coordinatorScheduleRepository;
    private final TourCheckpointRepository tourCheckpointRepository;
    private final SessionCheckpointLogRepository sessionCheckpointLogRepository;

    @Override
    @Transactional
    public TourSessionStartResponse startSession(UUID coordinatorId, UUID sessionId, SessionCheckpointLogRequest request) {
        log.info("Attempting to start tour session with ID: {} by coordinator ID: {} with coordinates: [lat: {}, lon: {}]",
                sessionId, coordinatorId, request.getLatitude(), request.getLongitude());

        TourSession tourSession = tourSessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> {
                    log.warn("Tour session with ID {} not found", sessionId);
                    return new AppException(ErrorCode.SESSION_NOT_FOUND);
                });

        CoordinatorSchedule schedule = coordinatorScheduleRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, coordinatorId)
                .orElseThrow(() -> {
                    log.warn("Coordinator {} is not assigned to tour session {}", coordinatorId, sessionId);
                    return new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
                });

        if (Boolean.TRUE.equals(schedule.getIsCancelled())) {
            log.warn("Schedule assignment for coordinator {} in session {} is cancelled", coordinatorId, sessionId);
            throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
        }

        if (!Boolean.TRUE.equals(schedule.getIsLead())) {
            log.warn("Coordinator {} is not the lead coordinator for session {}", coordinatorId, sessionId);
            throw new AppException(ErrorCode.NOT_LEAD_COORDINATOR);
        }

        if (tourSession.getStatus() == TourSessionStatus.IN_PROGRESS) {
            log.warn("Tour session {} is already in progress", sessionId);
            throw new AppException(ErrorCode.SESSION_ALREADY_STARTED);
        }

        if (tourSession.getStatus() == TourSessionStatus.COMPLETED) {
            log.warn("Tour session {} is already completed", sessionId);
            throw new AppException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }

        if (tourSession.getStatus() == TourSessionStatus.CANCELLED) {
            log.warn("Tour session {} is already cancelled", sessionId);
            throw new AppException(ErrorCode.SESSION_ALREADY_CANCELLED);
        }

        LocalDateTime now = LocalDateTime.now();
        Tour tour = tourSession.getTourSchedule().getTour();
        List<TourCheckpoint> checkpoints = tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);

        if (!checkpoints.isEmpty()) {
            TourCheckpoint startCheckpoint = checkpoints.get(0);
            if (startCheckpoint.getLatitude() != null && startCheckpoint.getLongitude() != null) {
                if (!GeoUtils.isWithinAllowedRadius(request.getLatitude(), request.getLongitude(), startCheckpoint.getLatitude(), startCheckpoint.getLongitude())) {
                    log.warn("Coordinator is too far from start checkpoint. Max allowed: {} meters", com.sep.treksphere.constant.ValidationConstant.ALLOWED_CHECKIN_RADIUS_METERS);
                    throw new AppException(ErrorCode.CHECKIN_OUT_OF_RANGE);
                }
            }
        }

        tourSession.setStatus(TourSessionStatus.IN_PROGRESS);
        tourSession.setStartedAt(now);
        tourSessionRepository.save(tourSession);
        log.info("Tour session {} successfully updated to IN_PROGRESS at {}", sessionId, now);

        List<SessionCheckpointLog> logs = new ArrayList<>();

        for (int i = 0; i < checkpoints.size(); i++) {
            TourCheckpoint checkpoint = checkpoints.get(i);
            SessionCheckpointLog checkpointLog = new SessionCheckpointLog();
            checkpointLog.setTourSession(tourSession);
            checkpointLog.setCheckpoint(checkpoint);
            checkpointLog.setIsDeleted(false);

            if (i == 0) {
                checkpointLog.setStatus(SessionCheckpointLogStatus.REACHED);
                checkpointLog.setReachedAt(now);
                checkpointLog.setActualLatitude(request.getLatitude());
                checkpointLog.setActualLongitude(request.getLongitude());
                checkpointLog.setNote(request.getNote());
            } else {
                checkpointLog.setStatus(SessionCheckpointLogStatus.PENDING);
            }
            logs.add(checkpointLog);
        }

        if (!logs.isEmpty()) {
            sessionCheckpointLogRepository.saveAll(logs);
            log.info("Initialized {} session checkpoint logs for tour session {}", logs.size(), sessionId);
        } else {
            log.warn("No checkpoints found for tour ID: {} in session {}", tour.getTourId(), sessionId);
        }

        return TourSessionStartResponse.builder()
                .tourSessionId(tourSession.getTourSessionId())
                .status(tourSession.getStatus())
                .startedAt(tourSession.getStartedAt())
                .build();
    }

    @Override
    @Transactional
    public SessionCheckpointLogResponse checkinCheckpoint(UUID coordinatorId, UUID sessionId, SessionCheckpointLogRequest request) {
        log.info("Attempting checkpoint check-in for tour session ID: {} by coordinator ID: {} with coordinates: [lat: {}, lon: {}]",
                sessionId, coordinatorId, request.getLatitude(), request.getLongitude());

        TourSession tourSession = tourSessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> {
                    log.warn("Tour session with ID {} not found", sessionId);
                    return new AppException(ErrorCode.SESSION_NOT_FOUND);
                });

        CoordinatorSchedule schedule = coordinatorScheduleRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, coordinatorId)
                .orElseThrow(() -> {
                    log.warn("Coordinator {} is not assigned to tour session {}", coordinatorId, sessionId);
                    return new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
                });

        if (Boolean.TRUE.equals(schedule.getIsCancelled())) {
            log.warn("Schedule assignment for coordinator {} in session {} is cancelled", coordinatorId, sessionId);
            throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
        }

        if (tourSession.getStatus() != TourSessionStatus.IN_PROGRESS) {
            log.warn("Tour session {} is not in progress. Current status: {}", sessionId, tourSession.getStatus());
            throw new AppException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        }

        List<SessionCheckpointLog> pendingLogs = sessionCheckpointLogRepository
                .findByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCheckpoint_CheckpointOrderAsc(
                        sessionId, SessionCheckpointLogStatus.PENDING
                );

        if (pendingLogs.isEmpty()) {
            log.warn("All checkpoints have already been reached for tour session {}", sessionId);
            throw new AppException(ErrorCode.NO_PENDING_CHECKPOINTS);
        }

        SessionCheckpointLog nextLog = pendingLogs.get(0);
        TourCheckpoint checkpoint = nextLog.getCheckpoint();

        if (checkpoint.getLatitude() != null && checkpoint.getLongitude() != null) {
            if (!GeoUtils.isWithinAllowedRadius(
                    request.getLatitude(), request.getLongitude(),
                    checkpoint.getLatitude(), checkpoint.getLongitude())) {
                log.warn("Coordinator {} is too far from checkpoint '{}' (order: {}). Max allowed: {} meters",
                        coordinatorId, checkpoint.getCheckpointName(), checkpoint.getCheckpointOrder(), ValidationConstant.ALLOWED_CHECKIN_RADIUS_METERS);
                throw new AppException(ErrorCode.CHECKIN_OUT_OF_RANGE);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        nextLog.setStatus(SessionCheckpointLogStatus.REACHED);
        nextLog.setReachedAt(now);
        nextLog.setActualLatitude(request.getLatitude());
        nextLog.setActualLongitude(request.getLongitude());
        nextLog.setNote(request.getNote());

        sessionCheckpointLogRepository.save(nextLog);
        log.info("Checkpoint '{}' (order: {}) for session {} successfully marked as REACHED at {}",
                checkpoint.getCheckpointName(), checkpoint.getCheckpointOrder(), sessionId, now);

        return SessionCheckpointLogResponse.builder()
                .sessionCheckpointLogId(nextLog.getSessionCheckpointLogId())
                .checkpointId(checkpoint.getCheckpointId())
                .checkpointName(checkpoint.getCheckpointName())
                .checkpointOrder(checkpoint.getCheckpointOrder())
                .status(nextLog.getStatus())
                .reachedAt(nextLog.getReachedAt())
                .build();
    }
}
