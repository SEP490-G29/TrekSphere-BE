package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface MatchingGroupService {
    PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter);

    MatchingGroupDetailResponse getMatchingGroupById(UUID id);

    MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, CustomUserDetails userDetails);

    MatchingMemberResponse joinMatchingGroup(UUID groupId, CustomUserDetails userDetails);

    MatchingMemberResponse approveMember(UUID memberId, CustomUserDetails userDetails);
}

