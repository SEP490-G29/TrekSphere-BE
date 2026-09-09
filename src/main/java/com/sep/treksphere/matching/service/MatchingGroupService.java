package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.GroupApplicationRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupUpdateRequest;
import com.sep.treksphere.matching.dto.request.MatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.request.MyMatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.request.MyMatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.matching.dto.response.MatchingGroupResponse;
import com.sep.treksphere.matching.dto.response.MatchingMemberResponse;
import com.sep.treksphere.matching.dto.response.MyMatchingJoinRequestResponse;

import java.util.UUID;

public interface MatchingGroupService {

    PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter);

    PaginationResponse<MatchingGroupResponse> getMyMatchingGroups(
            MyMatchingGroupFilterRequest filter,
            CustomUserDetails userDetails
    );

    MatchingGroupDetailResponse getMatchingGroupById(UUID id, CustomUserDetails userDetails);

    MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, UUID userId);

    MatchingMemberResponse submitApplication(
            UUID groupId,
            GroupApplicationRequest request,
            CustomUserDetails userDetails
    );

    PaginationResponse<MatchingMemberResponse> getJoinRequests(
            UUID groupId,
            MatchingJoinRequestFilter filter,
            CustomUserDetails userDetails
    );

    PaginationResponse<MyMatchingJoinRequestResponse> getMyJoinRequests(
            MyMatchingJoinRequestFilter filter,
            CustomUserDetails userDetails
    );

    MatchingMemberResponse approveMember(
            UUID groupId,
            UUID applicationId,
            CustomUserDetails userDetails
    );

    MatchingMemberResponse rejectMember(
            UUID groupId,
            UUID applicationId,
            CustomUserDetails userDetails
    );

    MatchingMemberResponse withdrawApplication(UUID groupId, CustomUserDetails userDetails);

    MatchingMemberResponse leaveMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    void disbandMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    MatchingGroupDetailResponse updateMatchingGroup(
            UUID groupId,
            MatchingGroupUpdateRequest request,
            CustomUserDetails userDetails
    );

    MatchingGroupDetailResponse hideMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    MatchingGroupDetailResponse showMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    MatchingGroupDetailResponse closeMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    MatchingGroupDetailResponse openMatchingGroup(UUID groupId, CustomUserDetails userDetails);
}
