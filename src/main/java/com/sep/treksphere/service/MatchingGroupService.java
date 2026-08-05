package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;
import java.util.List;

public interface MatchingGroupService {
    PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter);

    List<MatchingGroupResponse> getMyMatchingGroups(CustomUserDetails userDetails);

    MatchingGroupDetailResponse getMatchingGroupById(UUID id);

    MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, CustomUserDetails userDetails);

    MatchingMemberResponse joinMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    PaginationResponse<MatchingMemberResponse> getJoinRequests(
            UUID groupId,
            JoinStatus status,
            int page,
            int size,
            CustomUserDetails userDetails
    );

    MatchingMemberResponse approveMember(UUID memberId, CustomUserDetails userDetails);

    MatchingMemberResponse rejectMember(UUID memberId, CustomUserDetails userDetails);

    MatchingMemberResponse leaveMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    void disbandMatchingGroup(UUID groupId, CustomUserDetails userDetails);
}

