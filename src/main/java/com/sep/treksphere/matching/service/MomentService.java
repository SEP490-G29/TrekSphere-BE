package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.matching.dto.request.MomentCreateRequest;
import com.sep.treksphere.matching.dto.request.MomentFilterRequest;
import com.sep.treksphere.matching.dto.request.MomentHideRequest;
import com.sep.treksphere.matching.dto.request.MomentUpdateRequest;
import com.sep.treksphere.matching.dto.request.MomentVisibilityUpdateRequest;
import com.sep.treksphere.matching.dto.response.MomentMapResponse;
import com.sep.treksphere.matching.dto.response.MomentMediaResponse;
import com.sep.treksphere.matching.dto.response.MomentResponse;

import java.util.List;
import java.util.UUID;

public interface MomentService {

    // Group Moments
    MomentResponse createGroupMoment(UUID groupId, UUID currentUserId, MomentCreateRequest request);

    MomentResponse updateGroupMoment(UUID groupId, UUID momentId, UUID currentUserId, MomentUpdateRequest request);

    void deleteGroupMoment(UUID groupId, UUID momentId, UUID currentUserId);

    MomentResponse updateGroupMomentVisibility(UUID groupId, UUID momentId, UUID currentUserId, MomentVisibilityUpdateRequest request);

    MomentResponse hideGroupMoment(UUID groupId, UUID momentId, UUID currentUserId, MomentHideRequest request);

    MomentResponse unhideGroupMoment(UUID groupId, UUID momentId, UUID currentUserId);

    PaginationResponse<MomentResponse> getGroupMoments(UUID groupId, UUID currentUserId, MomentFilterRequest filter);

    PaginationResponse<MomentMediaResponse> getGroupAlbum(UUID groupId, UUID currentUserId, BaseFilterRequest filter);

    List<MomentMapResponse> getGroupMomentsMap(UUID groupId, UUID currentUserId);

    MomentResponse getGroupMomentDetail(UUID groupId, UUID momentId, UUID currentUserId);

    // Personal Moments & Showcase
    MomentResponse createPersonalMoment(UUID currentUserId, MomentCreateRequest request);

    MomentResponse updatePersonalMoment(UUID momentId, UUID currentUserId, MomentUpdateRequest request);

    void deletePersonalMoment(UUID momentId, UUID currentUserId);

    MomentResponse updatePersonalMomentVisibility(UUID momentId, UUID currentUserId, MomentVisibilityUpdateRequest request);

    PaginationResponse<MomentResponse> getMyMoments(UUID currentUserId, MomentFilterRequest filter);

    List<MomentMapResponse> getMyMomentsMap(UUID currentUserId);

    PaginationResponse<MomentResponse> getUserPublicMoments(UUID targetUserId, MomentFilterRequest filter);

    List<MomentMapResponse> getUserPublicMomentsMap(UUID targetUserId);

    MomentResponse getMomentDetail(UUID momentId, UUID currentUserId);
}

