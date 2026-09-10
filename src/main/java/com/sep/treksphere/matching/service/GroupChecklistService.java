package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.GroupChecklistFilterRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemStatusUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupChecklistItemResponse;
import com.sep.treksphere.matching.dto.response.GroupChecklistSummaryResponse;

import java.util.UUID;

public interface GroupChecklistService {

    GroupChecklistSummaryResponse getChecklistSummary(
            UUID groupId, GroupChecklistFilterRequest filter, UUID currentUserId);

    GroupChecklistItemResponse createChecklistItem(
            UUID groupId, GroupChecklistItemCreateRequest request, UUID currentUserId);

    GroupChecklistItemResponse updateChecklistItem(
            UUID groupId, UUID itemId, GroupChecklistItemUpdateRequest request, UUID currentUserId);

    GroupChecklistItemResponse updateItemStatus(
            UUID groupId, UUID itemId, GroupChecklistItemStatusUpdateRequest request, UUID currentUserId);

    void deleteChecklistItem(UUID groupId, UUID itemId, UUID currentUserId);
}
