package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.SessionCheckpointLogRequest;
import com.sep.treksphere.dto.response.SessionCheckpointLogResponse;
import com.sep.treksphere.dto.response.TourSessionEndResponse;
import com.sep.treksphere.dto.response.TourSessionStartResponse;

import java.util.UUID;

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

    TourSessionEndResponse endSession(
            UUID coordinatorId,
            UUID sessionId,
            SessionCheckpointLogRequest request
    );
}
