package com.sep.treksphere.service.impl;

import com.sep.treksphere.constant.ValidationConstant;
import com.sep.treksphere.dto.request.ParticipantAttendanceItem;
import com.sep.treksphere.dto.request.SessionCheckpointLogRequest;
import com.sep.treksphere.dto.request.TourSessionAttendanceRequest;
import com.sep.treksphere.dto.request.SessionEquipmentCheckRequest;
import com.sep.treksphere.dto.response.ParticipantAttendanceResponseItem;
import com.sep.treksphere.dto.response.SessionCheckpointLogResponse;
import com.sep.treksphere.dto.response.TourSessionAttendanceResponse;
import com.sep.treksphere.dto.response.TourSessionEndResponse;
import com.sep.treksphere.dto.response.TourSessionStartResponse;
import com.sep.treksphere.dto.response.SessionEquipmentCheckResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.tour.AttendanceType;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingServiceImpl implements TrackingService {

    private final TourSessionRepository tourSessionRepository;
    private final CoordinatorScheduleRepository coordinatorScheduleRepository;
    private final TourCheckpointRepository tourCheckpointRepository;
    private final SessionCheckpointLogRepository sessionCheckpointLogRepository;
    private final BookingParticipantRepository bookingParticipantRepository;
    private final SessionEquipmentRepository sessionEquipmentRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final UserRepository userRepository;

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
                    log.warn("Coordinator is too far from start checkpoint. Max allowed: {} meters", ValidationConstant.ALLOWED_CHECKIN_RADIUS_METERS);
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

    @Override
    @Transactional
    public TourSessionEndResponse endSession(UUID coordinatorId, UUID sessionId, SessionCheckpointLogRequest request) {
        log.info("Attempting to end tour session with ID: {} by coordinator ID: {} with destination coordinates: [lat: {}, lon: {}]",
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

        if (tourSession.getStatus() != TourSessionStatus.IN_PROGRESS) {
            log.warn("Tour session {} is not in progress. Current status: {}", sessionId, tourSession.getStatus());
            throw new AppException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        }

        LocalDateTime now = LocalDateTime.now();

        List<SessionCheckpointLog> allLogs = sessionCheckpointLogRepository
                .findByTourSession_TourSessionIdAndIsDeletedFalseOrderByCheckpoint_CheckpointOrderAsc(sessionId);

        if (!allLogs.isEmpty()) {
            SessionCheckpointLog destinationLog = allLogs.get(allLogs.size() - 1);
            TourCheckpoint destinationCheckpoint = destinationLog.getCheckpoint();

            if (destinationCheckpoint.getLatitude() != null && destinationCheckpoint.getLongitude() != null) {
                if (!GeoUtils.isWithinAllowedRadius(
                        request.getLatitude(), request.getLongitude(),
                        destinationCheckpoint.getLatitude(), destinationCheckpoint.getLongitude())) {
                    log.warn("Coordinator is too far from destination checkpoint '{}'. Max allowed: {} meters",
                            destinationCheckpoint.getCheckpointName(), ValidationConstant.ALLOWED_CHECKIN_RADIUS_METERS);
                    throw new AppException(ErrorCode.CHECKIN_OUT_OF_RANGE);
                }
            }

            destinationLog.setStatus(SessionCheckpointLogStatus.REACHED);
            destinationLog.setReachedAt(now);
            destinationLog.setActualLatitude(request.getLatitude());
            destinationLog.setActualLongitude(request.getLongitude());
            destinationLog.setNote(request.getNote());

            for (int i = 0; i < allLogs.size() - 1; i++) {
                SessionCheckpointLog logItem = allLogs.get(i);
                if (logItem.getStatus() == SessionCheckpointLogStatus.PENDING) {
                    logItem.setStatus(SessionCheckpointLogStatus.SKIPPED);
                    log.info("Checkpoint '{}' (order: {}) automatically marked as SKIPPED due to session completion",
                            logItem.getCheckpoint().getCheckpointName(), logItem.getCheckpoint().getCheckpointOrder());
                }
            }

            sessionCheckpointLogRepository.saveAll(allLogs);
        }

        tourSession.setStatus(TourSessionStatus.COMPLETED);
        tourSession.setEndedAt(now);
        tourSessionRepository.save(tourSession);
        log.info("Tour session {} successfully completed at {}", sessionId, now);

        return TourSessionEndResponse.builder()
                .tourSessionId(tourSession.getTourSessionId())
                .status(tourSession.getStatus())
                .endedAt(tourSession.getEndedAt())
                .build();
    }

    @Override
    @Transactional
    public TourSessionAttendanceResponse recordAttendance(UUID coordinatorId, UUID sessionId, TourSessionAttendanceRequest request) {
        log.info("Attempting to record attendance (type: {}) for tour session ID: {} by coordinator ID: {}",
                request.getAttendanceType(), sessionId, coordinatorId);

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

        List<BookingParticipant> activeParticipants = bookingParticipantRepository
                .findActiveParticipantsByScheduleId(tourSession.getTourSchedule().getScheduleId());

        Map<UUID, BookingParticipant> activeParticipantMap = activeParticipants.stream()
                .collect(Collectors.toMap(BookingParticipant::getParticipantId, p -> p));

        LocalDateTime now = LocalDateTime.now();
        List<ParticipantAttendanceResponseItem> responseItems = new ArrayList<>();
        List<BookingParticipant> toSave = new ArrayList<>();

        for (ParticipantAttendanceItem item : request.getParticipants()) {
            BookingParticipant participant = activeParticipantMap.get(item.getParticipantId());
            if (participant == null) {
                log.warn("Participant with ID {} is not part of tour session {}", item.getParticipantId(), sessionId);
                throw new AppException(ErrorCode.PARTICIPANT_NOT_FOUND_IN_SESSION);
            }

            if (request.getAttendanceType() == AttendanceType.START) {
                participant.setIsPresentStart(item.getIsPresent());
                participant.setStartAttendedAt(now);
            } else if (request.getAttendanceType() == AttendanceType.END) {
                participant.setIsPresentEnd(item.getIsPresent());
                participant.setEndAttendedAt(now);
            } else {
                throw new AppException(ErrorCode.INVALID_ATTENDANCE_TYPE);
            }

            toSave.add(participant);

            responseItems.add(ParticipantAttendanceResponseItem.builder()
                    .participantId(participant.getParticipantId())
                    .fullName(participant.getFullName())
                    .isPresent(item.getIsPresent())
                    .build());
        }

        bookingParticipantRepository.saveAll(toSave);
        log.info("Successfully recorded {} attendance records for session {}", toSave.size(), sessionId);

        return TourSessionAttendanceResponse.builder()
                .tourSessionId(tourSession.getTourSessionId())
                .attendanceType(request.getAttendanceType())
                .recordedAt(now)
                .participants(responseItems)
                .build();
    }

    @Override
    @Transactional
    public SessionEquipmentCheckResponse checkEquipment(UUID userId, UUID sessionEquipmentId, SessionEquipmentCheckRequest request) {
        log.info("User {} is attempting to check session equipment allocation {}", userId, sessionEquipmentId);

        SessionEquipment sessionEquipment = sessionEquipmentRepository
                .findBySessionEquipmentIdAndIsDeletedFalse(sessionEquipmentId)
                .orElseThrow(() -> {
                    log.warn("Session equipment allocation {} not found", sessionEquipmentId);
                    return new AppException(ErrorCode.SESSION_EQUIPMENT_NOT_FOUND);
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User {} not found", userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        boolean isAuthorized = false;

        Optional<VendorStaff> staffOpt = vendorStaffRepository
                .findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(userId);
        if (staffOpt.isPresent()) {
            VendorStaff staff = staffOpt.get();
            UUID staffVendorId = staff.getVendor().getVendorId();
            UUID equipmentVendorId = sessionEquipment.getEquipment().getVendor().getVendorId();

            if (staffVendorId.equals(equipmentVendorId)) {
                isAuthorized = true;
                log.info("User {} authorized as VENDOR_STAFF for vendor {}", userId, staffVendorId);
            }
        }

        if (!isAuthorized) {
            UUID sessionId = sessionEquipment.getTourSession().getTourSessionId();
            Optional<CoordinatorSchedule> scheduleOpt = coordinatorScheduleRepository
                    .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, userId);

            if (scheduleOpt.isPresent()) {
                CoordinatorSchedule schedule = scheduleOpt.get();
                if (!Boolean.TRUE.equals(schedule.getIsCancelled())) {
                    isAuthorized = true;
                    log.info("User {} authorized as COORDINATOR for session {}", userId, sessionId);
                }
            }
        }

        if (!isAuthorized) {
            log.warn("User {} is not authorized to check session equipment allocation {}", userId, sessionEquipmentId);
            throw new AppException(ErrorCode.UNAUTHORIZED_EQUIPMENT_CHECK);
        }

        sessionEquipment.setIsChecked(request.getIsChecked());
        sessionEquipment.setNote(request.getNote());
        sessionEquipment.setCheckedBy(user);
        sessionEquipment.setUpdatedAt(LocalDateTime.now());

        sessionEquipmentRepository.save(sessionEquipment);
        log.info("Session equipment allocation {} checked status successfully set to {} by user {}",
                sessionEquipmentId, request.getIsChecked(), userId);

        return SessionEquipmentCheckResponse.builder()
                .sessionEquipmentId(sessionEquipment.getSessionEquipmentId())
                .tourSessionId(sessionEquipment.getTourSession().getTourSessionId())
                .equipmentId(sessionEquipment.getEquipment().getEquipmentId())
                .equipmentName(sessionEquipment.getEquipment().getEquipmentName())
                .quantity(sessionEquipment.getQuantity())
                .isChecked(sessionEquipment.getIsChecked())
                .checkedById(user.getUserId())
                .checkedByName(user.getFullName())
                .note(sessionEquipment.getNote())
                .updatedAt(sessionEquipment.getUpdatedAt())
                .build();
    }
}
