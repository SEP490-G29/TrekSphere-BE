package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.response.TrackingSnapshotResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.TrackingSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackingSnapshotServiceImpl implements TrackingSnapshotService {

    private final TourSessionRepository tourSessionRepository;
    private final BookingParticipantRepository participantRepository;
    private final SessionEquipmentRepository equipmentRepository;
    private final SessionCheckpointLogRepository checkpointLogRepository;
    private final TourCheckpointRepository checkpointRepository;
    private final SosAlertRepository sosAlertRepository;
    private final TrackingSessionRevisionRepository revisionRepository;

    @Override
    @Transactional(readOnly = true)
    public TrackingSnapshotResponse getSnapshot(UUID sessionId) {
        TourSession session = tourSessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        List<BookingParticipant> participants = participantRepository.findActiveParticipantsByScheduleIdAndStatuses(
                session.getTourSchedule().getScheduleId(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED)
        );
        List<SessionEquipment> equipments = equipmentRepository
                .findByTourSession_TourSessionIdAndIsDeletedFalse(sessionId);

        List<SessionCheckpointLog> logs = checkpointLogRepository
                .findByTourSession_TourSessionIdAndIsDeletedFalseOrderByCheckpoint_CheckpointOrderAsc(sessionId);
        Map<UUID, SessionCheckpointLog> logsByCheckpoint = logs.stream()
                .collect(Collectors.toMap(log -> log.getCheckpoint().getCheckpointId(), Function.identity()));
        List<TourCheckpoint> checkpoints = checkpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(session.getTourSchedule().getTour());

        TrackingSnapshotResponse.SosState latestSos = sosAlertRepository
                .findFirstByTourSession_TourSessionIdAndIsDeletedFalseOrderByCreatedAtDesc(sessionId)
                .map(this::mapSos)
                .orElse(null);

        return TrackingSnapshotResponse.builder()
                .sessionId(sessionId)
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .revision(getRevision(sessionId))
                .participants(participants.stream().map(this::mapParticipant).toList())
                .equipments(equipments.stream().map(this::mapEquipment).toList())
                .checkpoints(checkpoints.stream().map(checkpoint -> {
                    SessionCheckpointLog log = logsByCheckpoint.get(checkpoint.getCheckpointId());
                    return TrackingSnapshotResponse.CheckpointState.builder()
                            .checkpointId(checkpoint.getCheckpointId())
                            .checkpointName(checkpoint.getCheckpointName())
                            .checkpointOrder(checkpoint.getCheckpointOrder())
                            .latitude(checkpoint.getLatitude())
                            .longitude(checkpoint.getLongitude())
                            .status(log == null ? SessionCheckpointLogStatus.PENDING : log.getStatus())
                            .reachedAt(log == null ? null : log.getReachedAt())
                            .build();
                }).toList())
                .latestSos(latestSos)
                .build();
    }

    @Override
    public long getRevision(UUID sessionId) {
        return revisionRepository.findById(sessionId).map(TrackingSessionRevision::getRevision).orElse(0L);
    }

    private TrackingSnapshotResponse.ParticipantState mapParticipant(BookingParticipant participant) {
        return TrackingSnapshotResponse.ParticipantState.builder()
                .participantId(participant.getParticipantId())
                .fullName(participant.getFullName())
                .isPresentStart(participant.getIsPresentStart())
                .startAttendedAt(participant.getStartAttendedAt())
                .isPresentEnd(participant.getIsPresentEnd())
                .endAttendedAt(participant.getEndAttendedAt())
                .build();
    }

    private TrackingSnapshotResponse.EquipmentState mapEquipment(SessionEquipment equipment) {
        return TrackingSnapshotResponse.EquipmentState.builder()
                .sessionEquipmentId(equipment.getSessionEquipmentId())
                .equipmentId(equipment.getEquipment().getEquipmentId())
                .equipmentName(equipment.getEquipment().getEquipmentName())
                .quantity(equipment.getQuantity())
                .isChecked(equipment.getIsChecked())
                .note(equipment.getNote())
                .build();
    }

    private TrackingSnapshotResponse.SosState mapSos(SosAlert sos) {
        return TrackingSnapshotResponse.SosState.builder()
                .sosAlertId(sos.getSosAlertId())
                .status(sos.getStatus())
                .latitude(sos.getLatitude())
                .longitude(sos.getLongitude())
                .message(sos.getMessage())
                .createdAt(sos.getCreatedAt())
                .build();
    }
}
