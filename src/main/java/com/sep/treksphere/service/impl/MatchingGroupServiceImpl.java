package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import com.sep.treksphere.mapper.MatchingGroupMapper;
import com.sep.treksphere.repository.MatchingGroupRepository;
import com.sep.treksphere.service.MatchingGroupService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingGroupServiceImpl implements MatchingGroupService {

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingGroupMapper matchingGroupMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter) {
        log.info("Fetching available matching groups with filters: tourId={}, targetDate={}", 
                filter.getTourId(), filter.getTargetDate());

        Page<MatchingGroup> groups = matchingGroupRepository.findAvailableMatchingGroups(
                MatchingGroupStatus.OPEN,
                filter.getTourId(), 
                filter.getTargetDate(), 
                filter.getPageable()
        );

        return PaginationUtils.toPaginationResponse(groups.map(matchingGroupMapper::toResponse));
    }
}
