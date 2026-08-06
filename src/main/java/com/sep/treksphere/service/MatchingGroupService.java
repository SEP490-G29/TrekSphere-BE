package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.request.MatchingJoinRequestFilter;
import com.sep.treksphere.dto.request.OwnedMatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface MatchingGroupService {
    PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter);

    PaginationResponse<MatchingGroupResponse> getOwnedMatchingGroups(
            OwnedMatchingGroupFilterRequest filter,
            CustomUserDetails userDetails
    );

    MatchingGroupDetailResponse getMatchingGroupById(UUID id, CustomUserDetails userDetails);

    MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, CustomUserDetails userDetails);

    MatchingMemberResponse joinMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    PaginationResponse<MatchingMemberResponse> getJoinRequests(
            UUID groupId,
            MatchingJoinRequestFilter filter,
            CustomUserDetails userDetails
    );

    MatchingMemberResponse approveMember(UUID groupId, UUID memberId, CustomUserDetails userDetails);

    MatchingMemberResponse rejectMember(UUID memberId, CustomUserDetails userDetails);

    MatchingMemberResponse leaveMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    void disbandMatchingGroup(UUID groupId, CustomUserDetails userDetails);
}

