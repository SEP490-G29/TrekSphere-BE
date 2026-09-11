package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.CustomJourneyActivityCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyActivityUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyActivityResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyCheckpointResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyDetailResponse;

import com.sep.treksphere.matching.dto.request.CustomJourneyCostItemCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCostItemUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyCostSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CustomJourneyService {

    CustomJourneyDetailResponse getJourneyByGroupId(UUID groupId, UUID currentUserId);

    CustomJourneyDetailResponse updateJourney(UUID groupId, CustomJourneyUpdateRequest request, UUID currentUserId);

    List<CustomJourneyCheckpointResponse> getCheckpoints(UUID groupId, UUID currentUserId);

    CustomJourneyCheckpointResponse createCheckpoint(UUID groupId, CustomJourneyCheckpointCreateRequest request, UUID currentUserId);

    CustomJourneyCheckpointResponse updateCheckpoint(UUID groupId, UUID checkpointId, CustomJourneyCheckpointUpdateRequest request, UUID currentUserId);

    void deleteCheckpoint(UUID groupId, UUID checkpointId, UUID currentUserId);

    List<CustomJourneyActivityResponse> getActivities(UUID groupId, UUID currentUserId);

    CustomJourneyActivityResponse createActivity(
            UUID groupId, CustomJourneyActivityCreateRequest request, UUID currentUserId);

    CustomJourneyActivityResponse updateActivity(
            UUID groupId, UUID activityId, CustomJourneyActivityUpdateRequest request, UUID currentUserId);

    void deleteActivity(UUID groupId, UUID activityId, UUID currentUserId);

    CustomJourneyCostSummaryResponse getCostSummary(UUID groupId, UUID currentUserId);

    List<CustomJourneyCostItemResponse> getCostItems(UUID groupId, UUID currentUserId);

    CustomJourneyCostItemResponse createCostItem(
            UUID groupId, CustomJourneyCostItemCreateRequest request, UUID currentUserId);

    CustomJourneyCostItemResponse updateCostItem(
            UUID groupId, UUID costItemId, CustomJourneyCostItemUpdateRequest request, UUID currentUserId);

    void deleteCostItem(UUID groupId, UUID costItemId, UUID currentUserId);
}

