package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyCheckpointResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyDetailResponse;

import java.util.List;
import java.util.UUID;

public interface CustomJourneyService {

    CustomJourneyDetailResponse getJourneyByGroupId(UUID groupId, UUID currentUserId);

    CustomJourneyDetailResponse updateJourney(UUID groupId, CustomJourneyUpdateRequest request, UUID currentUserId);

    List<CustomJourneyCheckpointResponse> getCheckpoints(UUID groupId, UUID currentUserId);

    CustomJourneyCheckpointResponse createCheckpoint(UUID groupId, CustomJourneyCheckpointCreateRequest request, UUID currentUserId);

    CustomJourneyCheckpointResponse updateCheckpoint(UUID groupId, UUID checkpointId, CustomJourneyCheckpointUpdateRequest request, UUID currentUserId);

    void deleteCheckpoint(UUID groupId, UUID checkpointId, UUID currentUserId);
}
