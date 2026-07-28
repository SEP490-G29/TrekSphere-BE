package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.PaginationResponse;

public interface MatchingGroupService {
    PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter);
}
