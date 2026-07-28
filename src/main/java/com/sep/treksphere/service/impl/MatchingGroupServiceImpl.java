package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.MatchingGroupMapper;
import com.sep.treksphere.repository.MatchingGroupRepository;
import com.sep.treksphere.service.MatchingGroupService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    @Override
    @Transactional(readOnly = true)
    public MatchingGroupDetailResponse getMatchingGroupById(UUID id) {
        log.info("Fetching matching group detail: id={}", id);

        MatchingGroup matchingGroup = matchingGroupRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        MatchingGroupDetailResponse response = matchingGroupMapper.toDetailResponse(matchingGroup);

        List<MatchingMemberResponse> acceptedMembers = matchingGroup.getMembers().stream()
                .filter(member -> member.getStatus() == JoinStatus.ACCEPTED
                        && !Boolean.TRUE.equals(member.getIsDeleted()))
                .map(matchingGroupMapper::toMemberResponse)
                .toList();

        response.setMembers(acceptedMembers);

        return response;
    }
}
