package com.sep.treksphere.service.impl;

import com.sep.treksphere.constant.ValidationConstant;
import com.sep.treksphere.dto.request.ParticipantAttendanceItem;
import com.sep.treksphere.dto.request.SessionCheckpointLogRequest;
import com.sep.treksphere.dto.request.TourSessionAttendanceRequest;
import com.sep.treksphere.dto.request.SessionEquipmentCheckRequest;
import com.sep.treksphere.dto.request.CreateSosAlertRequest;
import com.sep.treksphere.dto.response.ParticipantAttendanceResponseItem;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.SessionCheckpointLogResponse;
import com.sep.treksphere.dto.response.SessionCheckpointStatusResponse;
import com.sep.treksphere.dto.response.TourSessionAttendanceResponse;
import com.sep.treksphere.dto.response.TourSessionEndResponse;
import com.sep.treksphere.dto.response.TourSessionStartResponse;
import com.sep.treksphere.dto.response.SessionEquipmentCheckResponse;
import com.sep.treksphere.dto.response.SosAlertResponse;
import com.sep.treksphere.dto.response.TourSessionSosStatusResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.tour.AttendanceType;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import com.sep.treksphere.enums.tour.SosAlertStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.TrackingService;
import com.sep.treksphere.utils.GeoUtils;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingServiceImpl implements TrackingService {

    private final VendorRepository vendorRepository;

    private final TourSessionRepository tourSessionRepository;
    private final CoordinatorScheduleRepository coordinatorScheduleRepository;
    private final TourCheckpointRepository tourCheckpointRepository;
    private final SessionCheckpointLogRepository sessionCheckpointLogRepository;
    private final BookingParticipantRepository bookingParticipantRepository;
    private final SessionEquipmentRepository sessionEquipmentRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final SosAlertRepository sosAlertRepository;

    @Override
    @Transactional
    public TourSessionStartResponse startSession(UUID coordinatorId, UUID sessionId, SessionCheckpointLogRequest request) {
        log.info("Attempting to start tour session with ID: {} by coordinator ID: {} with coordinates: [lat: {}, lon: {}]",
                sessionId, coordinatorId, request.getLatitude(), request.getLongitude());

        TourSession tourSession = getActiveTourSessionForUpdate(sessionId);
        CoordinatorSchedule assignment = getActiveCoordinatorAssignment(sessionId, coordinatorId);
        requireLeadCoordinator(assignment, coordinatorId, sessionId);
        validateSessionCanStart(tourSession);
        validateScheduleCanStart(tourSession);

        if (sessionCheckpointLogRepository.existsByTourSession_TourSessionIdAndIsDeletedFalse(sessionId)) {
            log.warn("Checkpoint logs already exist for pending tour session {}", sessionId);
            throw new AppException(ErrorCode.SESSION_CHECKPOINT_LOGS_ALREADY_INITIALIZED);
        }

        Tour tour = tourSession.getTourSchedule().getTour();
        List<TourCheckpoint> checkpoints = getValidCheckpoints(tour, sessionId);
        validateStartAttendance(tourSession);
        validateEquipmentReadiness(sessionId);
        validateWithinCheckpointRadius(request, checkpoints.get(0), coordinatorId);

        LocalDateTime now = LocalDateTime.now();
        tourSession.setStatus(TourSessionStatus.IN_PROGRESS);
        tourSession.setStartedAt(now);
        tourSessionRepository.save(tourSession);
        bookingRepository.findBySchedule_ScheduleIdAndBookingStatusAndIsDeletedFalse(
                        tourSession.getTourSchedule().getScheduleId(), BookingStatus.CONFIRMED)
                .forEach(booking -> {
                    booking.setBookingStatus(BookingStatus.IN_PROGRESS);
                    bookingRepository.save(booking);
                });
        log.info("Tour session {} successfully updated to IN_PROGRESS at {}", sessionId, now);

        List<SessionCheckpointLog> logs = initializeCheckpointLogs(tourSession, checkpoints, request, now);
        sessionCheckpointLogRepository.saveAll(logs);
        log.info("Initialized {} session checkpoint logs for tour session {}", logs.size(), sessionId);

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

        TourSession tourSession = getActiveTourSession(sessionId);
        getActiveCoordinatorAssignment(sessionId, coordinatorId);

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

        validateWithinCheckpointRadius(request, checkpoint, coordinatorId);

        LocalDateTime now = LocalDateTime.now();
        nextLog.setStatus(SessionCheckpointLogStatus.REACHED);
        nextLog.setReachedAt(now);
        nextLog.setActualLatitude(request.getLatitude());
        nextLog.setActualLongitude(request.getLongitude());
        nextLog.setNote(request.getNote());

        sessionCheckpointLogRepository.save(nextLog);
        log.info("Checkpoint '{}' (order: {}) for session {} successfully marked as REACHED at {}",
                checkpoint.getCheckpointName(), checkpoint.getCheckpointOrder(), sessionId, now);

        return mapToSessionCheckpointLogResponse(nextLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionCheckpointStatusResponse> getSessionCheckpointLogs(UUID userId, UUID sessionId) {
        log.info("User {} is requesting checkpoint logs for tour session {}", userId, sessionId);

        TourSession tourSession = getActiveTourSession(sessionId);
        authorizeSessionCheckpointLogAccess(userId, tourSession);

        return sessionCheckpointLogRepository
                .findByTourSession_TourSessionIdAndIsDeletedFalseOrderByCheckpoint_CheckpointOrderAsc(sessionId)
                .stream()
                .map(this::mapToSessionCheckpointStatusResponse)
                .toList();
    }

    @Override
    @Transactional
    public TourSessionEndResponse endSession(UUID coordinatorId, UUID sessionId, SessionCheckpointLogRequest request) {
        log.info("Attempting to end tour session with ID: {} by coordinator ID: {} with destination coordinates: [lat: {}, lon: {}]",
                sessionId, coordinatorId, request.getLatitude(), request.getLongitude());

        TourSession tourSession = getActiveTourSession(sessionId);
        CoordinatorSchedule assignment = getActiveCoordinatorAssignment(sessionId, coordinatorId);
        requireLeadCoordinator(assignment, coordinatorId, sessionId);

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

            validateWithinCheckpointRadius(request, destinationCheckpoint, coordinatorId);

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
        bookingRepository.findBySchedule_ScheduleIdAndBookingStatusAndIsDeletedFalse(
                        tourSession.getTourSchedule().getScheduleId(), BookingStatus.IN_PROGRESS)
                .forEach(booking -> {
                    booking.setBookingStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(booking);
                });
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

        TourSession tourSession = getActiveTourSession(sessionId);
        getActiveCoordinatorAssignment(sessionId, coordinatorId);

        AttendanceType attendanceType = request.getAttendanceType();
        boolean isStartAllowed = attendanceType == AttendanceType.START
                && (tourSession.getStatus() == TourSessionStatus.PENDING
                || tourSession.getStatus() == TourSessionStatus.IN_PROGRESS);
        boolean isEndAllowed = attendanceType == AttendanceType.END
                && tourSession.getStatus() == TourSessionStatus.IN_PROGRESS;

        if (!isStartAllowed && !isEndAllowed) {
            log.warn("Attendance type {} is not allowed for tour session {} in status {}",
                    attendanceType, sessionId, tourSession.getStatus());
            throw new AppException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        }

        Set<UUID> uniqueParticipantIds = new HashSet<>();
        boolean hasDuplicateParticipant = request.getParticipants().stream()
                .map(ParticipantAttendanceItem::getParticipantId)
                .anyMatch(participantId -> !uniqueParticipantIds.add(participantId));
        if (hasDuplicateParticipant) {
            log.warn("Attendance request for tour session {} contains duplicate participant IDs", sessionId);
            throw new AppException(ErrorCode.DUPLICATE_PARTICIPANT_IN_ATTENDANCE);
        }

        List<BookingParticipant> activeParticipants = bookingParticipantRepository
                .findActiveParticipantsByScheduleId(
                        tourSession.getTourSchedule().getScheduleId(),
                        BookingStatus.CONFIRMED
                );

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

            if (attendanceType == AttendanceType.START) {
                participant.setIsPresentStart(item.getIsPresent());
                participant.setStartAttendedAt(now);
            } else if (attendanceType == AttendanceType.END) {
                if (participant.getIsPresentStart() == null) {
                    log.warn("Participant {} has no START attendance for tour session {}",
                            participant.getParticipantId(), sessionId);
                    throw new AppException(ErrorCode.ATTENDANCE_START_REQUIRED);
                }
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
                .attendanceType(attendanceType)
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

    @Override
    @Transactional
    public SosAlertResponse createSosAlert(UUID senderId, CreateSosAlertRequest request) {
        log.info("User {} is attempting to trigger SOS alert for tour session ID: {}", senderId, request.getTourSessionId());

        TourSession tourSession = tourSessionRepository.findByTourSessionIdAndIsDeletedFalse(request.getTourSessionId())
                .orElseThrow(() -> {
                    log.warn("Tour session with ID {} not found", request.getTourSessionId());
                    return new AppException(ErrorCode.SESSION_NOT_FOUND);
                });

        if (tourSession.getStatus() != TourSessionStatus.IN_PROGRESS) {
            log.warn("Tour session {} is not in progress. Current status: {}", request.getTourSessionId(), tourSession.getStatus());
            throw new AppException(ErrorCode.SESSION_FOR_SOS_NOT_ACTIVE);
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> {
                    log.warn("Sender User {} not found", senderId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        UUID scheduleId = tourSession.getTourSchedule().getScheduleId();
        boolean isAuthorized = false;
        String senderRole = null;

        Optional<CoordinatorSchedule> coordinatorScheduleOpt = coordinatorScheduleRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(request.getTourSessionId(), senderId);

        if (coordinatorScheduleOpt.isPresent()) {
            CoordinatorSchedule schedule = coordinatorScheduleOpt.get();
            if (!Boolean.TRUE.equals(schedule.getIsCancelled())) {
                isAuthorized = true;
                senderRole = "COORDINATOR";
                log.info("SOS sender {} authorized as COORDINATOR for session {}", senderId, request.getTourSessionId());
            }
        }

        if (!isAuthorized) {
            boolean isBooker = bookingRepository
                    .existsByUser_UserIdAndSchedule_ScheduleIdAndBookingStatusAndIsDeletedFalse(
                            senderId, scheduleId, BookingStatus.CONFIRMED);

            if (isBooker) {
                isAuthorized = true;
                senderRole = "TREKKER";
                log.info("SOS sender {} authorized as TREKKER (Booker) for session {}", senderId, request.getTourSessionId());
            } else {
                String email = sender.getEmail();
                String phone = sender.getPhone();

                boolean isParticipant = false;
                if (email != null) {
                    isParticipant = bookingParticipantRepository
                            .existsByEmailAndBooking_Schedule_ScheduleIdAndBooking_BookingStatusAndIsDeletedFalse(
                                    email, scheduleId, BookingStatus.CONFIRMED);
                }

                if (!isParticipant && phone != null) {
                    isParticipant = bookingParticipantRepository
                            .existsByPhoneAndBooking_Schedule_ScheduleIdAndBooking_BookingStatusAndIsDeletedFalse(
                                    phone, scheduleId, BookingStatus.CONFIRMED);
                }

                if (isParticipant) {
                    isAuthorized = true;
                    senderRole = "TREKKER";
                    log.info("SOS sender {} authorized as TREKKER (Participant) for session {}", senderId, request.getTourSessionId());
                }
            }
        }

        if (!isAuthorized) {
            log.warn("User {} is not authorized to trigger SOS for session {}", senderId, request.getTourSessionId());
            throw new AppException(ErrorCode.UNAUTHORIZED_SOS_ALERT);
        }

        SosAlert sosAlert = new SosAlert();
        sosAlert.setTourSession(tourSession);
        sosAlert.setSender(sender);
        sosAlert.setLatitude(request.getLatitude());
        sosAlert.setLongitude(request.getLongitude());
        sosAlert.setMessage(request.getMessage());
        sosAlert.setStatus(SosAlertStatus.PENDING);

        sosAlertRepository.save(sosAlert);
        log.info("SOS alert successfully registered with ID {} for session {}", sosAlert.getSosAlertId(), request.getTourSessionId());

        return mapToSosAlertResponse(sosAlert);
    }

    @Override
    @Transactional(readOnly = true)
    public TourSessionSosStatusResponse getTourSessionSosStatus(UUID coordinatorId, UUID sessionId) {
        log.info("Coordinator {} is requesting SOS status for tour session {}", coordinatorId, sessionId);

        getActiveTourSession(sessionId);
        getActiveCoordinatorAssignment(sessionId, coordinatorId);

        Optional<SosAlert> pendingAlert = sosAlertRepository
                .findFirstByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                        sessionId,
                        SosAlertStatus.PENDING
                );

        Optional<SosAlert> trackedAlert = pendingAlert.isPresent()
                ? pendingAlert
                : sosAlertRepository.findFirstByTourSession_TourSessionIdAndIsDeletedFalseOrderByCreatedAtDesc(sessionId);

        SosAlertStatus status = trackedAlert.map(SosAlert::getStatus).orElse(null);
        return TourSessionSosStatusResponse.builder()
                .tourSessionId(sessionId)
                .hasSosAlert(trackedAlert.isPresent())
                .hasActiveSosAlert(pendingAlert.isPresent())
                .resolved(status == SosAlertStatus.RESOLVED)
                .status(status)
                .sosAlert(trackedAlert.map(this::mapToSosAlertResponse).orElse(null))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<SosAlertResponse> getActiveSosAlerts(UUID userId, Pageable pageable) {
        log.info("User {} is requesting active SOS alerts", userId);

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User {} not found", userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("ADMIN"));
        boolean isVendorManager = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("VENDOR_MANAGER"));

        UUID filterVendorId;

        if (isAdmin) {
            filterVendorId = null;
        } else if (isVendorManager) {
            Vendor vendor = vendorRepository.findByManager_UserId(userId)
                    .orElseThrow(() -> {
                        log.warn("Vendor manager User {} does not manage any vendor", userId);
                        return new AppException(ErrorCode.VENDOR_NOT_FOUND);
                    });
            filterVendorId = vendor.getVendorId();
            log.info("Filtering active SOS alerts for vendor {}", filterVendorId);
        } else {
            log.warn("User {} is not authorized to view active SOS alerts", userId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Page<SosAlert> alerts = sosAlertRepository.findAlertsByStatus(SosAlertStatus.PENDING, filterVendorId, pageable);

        return PaginationUtils.toPaginationResponse(alerts.map(this::mapToSosAlertResponse));
    }

    @Override
    @Transactional
    public SosAlertResponse resolveSosAlert(UUID userId, UUID sosId) {
        log.info("User {} is attempting to resolve SOS alert {}", userId, sosId);

        SosAlert sosAlert = sosAlertRepository.findById(sosId)
                .orElseThrow(() -> {
                    log.warn("SosAlert {} not found", sosId);
                    return new AppException(ErrorCode.SOS_ALERT_NOT_FOUND);
                });

        if (sosAlert.getStatus() == SosAlertStatus.RESOLVED) {
            log.warn("SosAlert {} is already resolved", sosId);
            throw new AppException(ErrorCode.SOS_ALERT_ALREADY_RESOLVED);
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User {} not found", userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("ADMIN"));
        boolean isVendorManager = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("VENDOR_MANAGER"));
        boolean isCoordinator = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("COORDINATOR"));

        boolean isAuthorized = false;

        if (isAdmin) {
            isAuthorized = true;
        } else if (isVendorManager) {
            Vendor vendor = vendorRepository.findByManager_UserId(userId)
                    .orElse(null);
            if (vendor != null) {
                UUID sosVendorId = sosAlert.getTourSession().getTourSchedule().getTour().getVendor().getVendorId();
                if (vendor.getVendorId().equals(sosVendorId)) {
                    isAuthorized = true;
                }
            }
        } else if (isCoordinator) {
            UUID sessionId = sosAlert.getTourSession().getTourSessionId();
            Optional<CoordinatorSchedule> scheduleOpt = coordinatorScheduleRepository
                    .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, userId);
            if (scheduleOpt.isPresent() && !Boolean.TRUE.equals(scheduleOpt.get().getIsCancelled())) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            log.warn("User {} is not authorized to resolve SOS alert {}", userId, sosId);
            throw new AppException(ErrorCode.UNAUTHORIZED_RESOLVE_SOS);
        }

        sosAlert.setStatus(SosAlertStatus.RESOLVED);
        sosAlert.setResolvedBy(currentUser);

        sosAlertRepository.save(sosAlert);
        log.info("SOS alert {} successfully resolved by user {}", sosId, userId);

        return mapToSosAlertResponse(sosAlert);
    }

    private TourSession getActiveTourSession(UUID sessionId) {
        return tourSessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> {
                    log.warn("Tour session with ID {} not found", sessionId);
                    return new AppException(ErrorCode.SESSION_NOT_FOUND);
                });
    }

    private TourSession getActiveTourSessionForUpdate(UUID sessionId) {
        return tourSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> {
                    log.warn("Tour session with ID {} not found", sessionId);
                    return new AppException(ErrorCode.SESSION_NOT_FOUND);
                });
    }

    private CoordinatorSchedule getActiveCoordinatorAssignment(UUID sessionId, UUID coordinatorId) {
        CoordinatorSchedule assignment = coordinatorScheduleRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, coordinatorId)
                .orElseThrow(() -> {
                    log.warn("Coordinator {} is not assigned to tour session {}", coordinatorId, sessionId);
                    return new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
                });

        if (Boolean.TRUE.equals(assignment.getIsCancelled())) {
            log.warn("Schedule assignment for coordinator {} in session {} is cancelled", coordinatorId, sessionId);
            throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
        }
        return assignment;
    }

    private void authorizeSessionCheckpointLogAccess(UUID userId, TourSession tourSession) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UUID sessionId = tourSession.getTourSessionId();
        UUID sessionVendorId = tourSession.getTourSchedule().getTour().getVendor().getVendorId();
        boolean authorized = false;

        if (hasRole(user, "VENDOR_MANAGER")) {
            authorized = vendorRepository.findByManager_UserId(userId)
                    .map(vendor -> vendor.getVendorId().equals(sessionVendorId))
                    .orElse(false);
        }

        if (!authorized && hasRole(user, "VENDOR_STAFF")) {
            authorized = vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(userId)
                    .map(staff -> staff.getVendor().getVendorId().equals(sessionVendorId))
                    .orElse(false);
        }

        if (!authorized && hasRole(user, "COORDINATOR")) {
            authorized = coordinatorScheduleRepository
                    .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, userId)
                    .filter(schedule -> !Boolean.TRUE.equals(schedule.getIsCancelled()))
                    .isPresent();
        }

        if (!authorized) {
            log.warn("User {} is not authorized to view checkpoint logs of tour session {}", userId, sessionId);
            throw new AppException(ErrorCode.UNAUTHORIZED_SESSION_ACCESS);
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getRoleName()));
    }

    private void requireLeadCoordinator(
            CoordinatorSchedule assignment,
            UUID coordinatorId,
            UUID sessionId
    ) {
        if (!Boolean.TRUE.equals(assignment.getIsLead())) {
            log.warn("Coordinator {} is not the lead coordinator for session {}", coordinatorId, sessionId);
            throw new AppException(ErrorCode.NOT_LEAD_COORDINATOR);
        }
    }

    private void validateSessionCanStart(TourSession tourSession) {
        UUID sessionId = tourSession.getTourSessionId();
        switch (tourSession.getStatus()) {
            case PENDING -> {
                return;
            }
            case IN_PROGRESS -> {
                log.warn("Tour session {} is already in progress", sessionId);
                throw new AppException(ErrorCode.SESSION_ALREADY_STARTED);
            }
            case COMPLETED -> {
                log.warn("Tour session {} is already completed", sessionId);
                throw new AppException(ErrorCode.SESSION_ALREADY_COMPLETED);
            }
            case CANCELLED -> {
                log.warn("Tour session {} is already cancelled", sessionId);
                throw new AppException(ErrorCode.SESSION_ALREADY_CANCELLED);
            }
        }
    }

    private void validateScheduleCanStart(TourSession tourSession) {
        TourSchedule schedule = tourSession.getTourSchedule();
        Tour tour = schedule == null ? null : schedule.getTour();
        if (schedule == null || tour == null
                || Boolean.TRUE.equals(schedule.getIsDeleted())
                || Boolean.TRUE.equals(tour.getIsDeleted())
                || (schedule.getStatus() != ScheduleStatus.OPEN
                && schedule.getStatus() != ScheduleStatus.CLOSED)) {
            log.warn("Schedule of tour session {} is not available for start", tourSession.getTourSessionId());
            throw new AppException(ErrorCode.SCHEDULE_NOT_AVAILABLE_FOR_START);
        }

        LocalDate today = LocalDate.now();
        if (schedule.getDepartureDate() == null
                || schedule.getReturnDate() == null
                || today.isBefore(schedule.getDepartureDate())
                || today.isAfter(schedule.getReturnDate())) {
            log.warn("Tour session {} cannot start on {}. Schedule range: {} - {}",
                    tourSession.getTourSessionId(), today,
                    schedule.getDepartureDate(), schedule.getReturnDate());
            throw new AppException(ErrorCode.SESSION_START_DATE_INVALID);
        }
    }

    private List<TourCheckpoint> getValidCheckpoints(Tour tour, UUID sessionId) {
        List<TourCheckpoint> checkpoints = tourCheckpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);
        if (checkpoints.size() < 2) {
            log.warn("Tour {} of session {} has fewer than two checkpoints", tour.getTourId(), sessionId);
            throw new AppException(ErrorCode.TOUR_CHECKPOINTS_NOT_CONFIGURED);
        }

        boolean hasInvalidCoordinates = checkpoints.stream()
                .anyMatch(checkpoint -> !hasValidCoordinates(checkpoint));
        if (hasInvalidCoordinates) {
            log.warn("Tour {} of session {} contains checkpoints without valid coordinates",
                    tour.getTourId(), sessionId);
            throw new AppException(ErrorCode.TOUR_CHECKPOINT_COORDINATES_REQUIRED);
        }
        return checkpoints;
    }

    private boolean hasValidCoordinates(TourCheckpoint checkpoint) {
        BigDecimal latitude = checkpoint.getLatitude();
        BigDecimal longitude = checkpoint.getLongitude();
        return latitude != null
                && longitude != null
                && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
                && latitude.compareTo(BigDecimal.valueOf(90)) <= 0
                && longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
                && longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    private void validateWithinCheckpointRadius(
            SessionCheckpointLogRequest request,
            TourCheckpoint checkpoint,
            UUID coordinatorId
    ) {
        if (!hasValidCoordinates(checkpoint)) {
            log.warn("Checkpoint {} does not have valid GPS coordinates", checkpoint.getCheckpointId());
            throw new AppException(ErrorCode.TOUR_CHECKPOINT_COORDINATES_REQUIRED);
        }
        if (!GeoUtils.isWithinAllowedRadius(
                request.getLatitude(), request.getLongitude(),
                checkpoint.getLatitude(), checkpoint.getLongitude())) {
            log.warn("Coordinator {} is too far from checkpoint '{}' (order: {}). Max allowed: {} meters",
                    coordinatorId, checkpoint.getCheckpointName(), checkpoint.getCheckpointOrder(),
                    ValidationConstant.ALLOWED_CHECKIN_RADIUS_METERS);
            throw new AppException(ErrorCode.CHECKIN_OUT_OF_RANGE);
        }
    }

    private void validateStartAttendance(TourSession tourSession) {
        List<BookingParticipant> participants = bookingParticipantRepository
                .findActiveParticipantsByScheduleId(
                        tourSession.getTourSchedule().getScheduleId(),
                        BookingStatus.CONFIRMED
                );
        if (participants.isEmpty()
                || participants.stream().anyMatch(participant -> participant.getIsPresentStart() == null)) {
            log.warn("START attendance is incomplete for tour session {}", tourSession.getTourSessionId());
            throw new AppException(ErrorCode.START_ATTENDANCE_INCOMPLETE);
        }
        if (participants.stream().noneMatch(participant -> Boolean.TRUE.equals(participant.getIsPresentStart()))) {
            log.warn("No participants are present for tour session {}", tourSession.getTourSessionId());
            throw new AppException(ErrorCode.NO_PRESENT_PARTICIPANTS);
        }
    }

    private void validateEquipmentReadiness(UUID sessionId) {
        boolean hasUncheckedEquipment = sessionEquipmentRepository
                .findByTourSession_TourSessionIdAndIsDeletedFalse(sessionId)
                .stream()
                .anyMatch(equipment -> !Boolean.TRUE.equals(equipment.getIsChecked()));
        if (hasUncheckedEquipment) {
            log.warn("Tour session {} still has unchecked equipment", sessionId);
            throw new AppException(ErrorCode.SESSION_EQUIPMENT_NOT_READY);
        }
    }

    private List<SessionCheckpointLog> initializeCheckpointLogs(
            TourSession tourSession,
            List<TourCheckpoint> checkpoints,
            SessionCheckpointLogRequest request,
            LocalDateTime reachedAt
    ) {
        List<SessionCheckpointLog> logs = new ArrayList<>(checkpoints.size());
        for (int index = 0; index < checkpoints.size(); index++) {
            SessionCheckpointLog checkpointLog = new SessionCheckpointLog();
            checkpointLog.setTourSession(tourSession);
            checkpointLog.setCheckpoint(checkpoints.get(index));

            if (index == 0) {
                checkpointLog.setStatus(SessionCheckpointLogStatus.REACHED);
                checkpointLog.setReachedAt(reachedAt);
                checkpointLog.setActualLatitude(request.getLatitude());
                checkpointLog.setActualLongitude(request.getLongitude());
                checkpointLog.setNote(request.getNote());
            } else {
                checkpointLog.setStatus(SessionCheckpointLogStatus.PENDING);
            }
            logs.add(checkpointLog);
        }
        return logs;
    }

    private SessionCheckpointLogResponse mapToSessionCheckpointLogResponse(SessionCheckpointLog checkpointLog) {
        TourCheckpoint checkpoint = checkpointLog.getCheckpoint();
        return SessionCheckpointLogResponse.builder()
                .sessionCheckpointLogId(checkpointLog.getSessionCheckpointLogId())
                .checkpointId(checkpoint.getCheckpointId())
                .checkpointName(checkpoint.getCheckpointName())
                .checkpointOrder(checkpoint.getCheckpointOrder())
                .status(checkpointLog.getStatus())
                .reachedAt(checkpointLog.getReachedAt())
                .build();
    }

    private SessionCheckpointStatusResponse mapToSessionCheckpointStatusResponse(SessionCheckpointLog checkpointLog) {
        TourCheckpoint checkpoint = checkpointLog.getCheckpoint();
        return SessionCheckpointStatusResponse.builder()
                .sessionCheckpointLogId(checkpointLog.getSessionCheckpointLogId())
                .tourSessionId(checkpointLog.getTourSession().getTourSessionId())
                .checkpointId(checkpoint.getCheckpointId())
                .checkpointName(checkpoint.getCheckpointName())
                .checkpointDescription(checkpoint.getDescription())
                .checkpointOrder(checkpoint.getCheckpointOrder())
                .checkpointLatitude(checkpoint.getLatitude())
                .checkpointLongitude(checkpoint.getLongitude())
                .checkpointAltitude(checkpoint.getAltitude())
                .checkpointImageUrl(checkpoint.getCheckpointImageUrl())
                .status(checkpointLog.getStatus())
                .build();
    }

    private SosAlertResponse mapToSosAlertResponse(SosAlert sosAlert) {
        String senderRole = "TREKKER";
        UUID senderId = sosAlert.getSender().getUserId();
        UUID sessionId = sosAlert.getTourSession().getTourSessionId();

        Optional<CoordinatorSchedule> coordinatorScheduleOpt = coordinatorScheduleRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, senderId);
        if (coordinatorScheduleOpt.isPresent()) {
            CoordinatorSchedule schedule = coordinatorScheduleOpt.get();
            if (!Boolean.TRUE.equals(schedule.getIsCancelled())) {
                senderRole = "COORDINATOR";
            }
        }

        UUID resolvedById = null;
        String resolvedByName = null;
        if (sosAlert.getResolvedBy() != null) {
            resolvedById = sosAlert.getResolvedBy().getUserId();
            resolvedByName = sosAlert.getResolvedBy().getFullName();
        }

        return SosAlertResponse.builder()
                .sosAlertId(sosAlert.getSosAlertId())
                .tourSessionId(sosAlert.getTourSession().getTourSessionId())
                .tourName(sosAlert.getTourSession().getTourSchedule().getTour().getTourName())
                .senderId(sosAlert.getSender().getUserId())
                .senderName(sosAlert.getSender().getFullName())
                .senderRole(senderRole)
                .latitude(sosAlert.getLatitude())
                .longitude(sosAlert.getLongitude())
                .message(sosAlert.getMessage())
                .status(sosAlert.getStatus())
                .createdAt(sosAlert.getCreatedAt())
                .resolvedById(resolvedById)
                .resolvedByName(resolvedByName)
                .build();
    }
}
