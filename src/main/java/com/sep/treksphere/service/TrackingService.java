package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.SessionCheckpointLogRequest;
import com.sep.treksphere.dto.request.TourSessionAttendanceRequest;
import com.sep.treksphere.dto.request.SessionEquipmentCheckRequest;
import com.sep.treksphere.dto.request.CreateSosAlertRequest;
import com.sep.treksphere.dto.response.SessionCheckpointLogResponse;
import com.sep.treksphere.dto.response.SessionCheckpointStatusResponse;
import com.sep.treksphere.dto.response.TourSessionEndResponse;
import com.sep.treksphere.dto.response.TourSessionStartResponse;
import com.sep.treksphere.dto.response.TourSessionAttendanceResponse;
import com.sep.treksphere.dto.response.SessionEquipmentCheckResponse;
import com.sep.treksphere.dto.response.SosAlertResponse;
import com.sep.treksphere.dto.response.PaginationResponse;

import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.List;

public interface TrackingService {

    TourSessionStartResponse startSession(
            UUID coordinatorId,
            UUID sessionId,
            SessionCheckpointLogRequest request
    );

    SessionCheckpointLogResponse checkinCheckpoint(
            UUID coordinatorId,
            UUID sessionId,
            SessionCheckpointLogRequest request
    );

    List<SessionCheckpointStatusResponse> getSessionCheckpointLogs(
            UUID userId,
            UUID sessionId
    );

    TourSessionEndResponse endSession(
            UUID coordinatorId,
            UUID sessionId,
            SessionCheckpointLogRequest request
    );

    TourSessionAttendanceResponse recordAttendance(
            UUID coordinatorId,
            UUID sessionId,
            TourSessionAttendanceRequest request
    );

    SessionEquipmentCheckResponse checkEquipment(
            UUID userId,
            UUID sessionEquipmentId,
            SessionEquipmentCheckRequest request
    );

    SosAlertResponse createSosAlert(
            UUID senderId,
            CreateSosAlertRequest request
    );

    PaginationResponse<SosAlertResponse> getActiveSosAlerts(
            UUID userId,
            Pageable pageable
    );

    SosAlertResponse resolveSosAlert(
            UUID userId,
            UUID sosId
    );
}
