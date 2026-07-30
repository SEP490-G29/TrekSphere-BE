package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.MatchingMember;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import com.sep.treksphere.enums.matching.MatchingRole;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.MatchingGroupMapper;
import com.sep.treksphere.repository.MatchingGroupRepository;
import com.sep.treksphere.repository.MatchingMemberRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.MatchingGroupService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingGroupServiceImpl implements MatchingGroupService {

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final TourRepository tourRepository;
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

    @Override
    @Transactional
    public MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Creating matching group: ownerId={}, groupName={}", currentUser.getUserId(), request.getGroupName());

        boolean hasActiveGroup = matchingGroupRepository.existsByOwnerAndStatusAndIsDeletedFalse(currentUser, MatchingGroupStatus.OPEN);
        if (hasActiveGroup) {
            throw new AppException(ErrorCode.ALREADY_HAS_ACTIVE_GROUP);
        }

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(request.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (request.getTargetDate().isBefore(LocalDate.now()) || request.getTargetDate().isEqual(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_TARGET_DATE);
        }

        if (request.getMatchingDeadline().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
        if (request.getMatchingDeadline().toLocalDate().isAfter(request.getTargetDate())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        MatchingGroup matchingGroup = new MatchingGroup();
        matchingGroup.setTour(tour);
        matchingGroup.setOwner(currentUser);
        matchingGroup.setGroupName(request.getGroupName());
        matchingGroup.setDescription(request.getDescription());
        matchingGroup.setMaxSize(request.getMaxSize());
        matchingGroup.setCurrentSize(1);
        matchingGroup.setTargetDate(request.getTargetDate());
        matchingGroup.setMatchingDeadline(request.getMatchingDeadline());
        matchingGroup.setStatus(MatchingGroupStatus.OPEN);

        MatchingMember ownerMember = new MatchingMember();
        ownerMember.setMatchingGroup(matchingGroup);
        ownerMember.setUser(currentUser);
        ownerMember.setRole(MatchingRole.OWNER);
        ownerMember.setStatus(JoinStatus.ACCEPTED);

        matchingGroup.getMembers().add(ownerMember);

        MatchingGroup savedGroup = matchingGroupRepository.save(matchingGroup);

        MatchingGroupDetailResponse response = matchingGroupMapper.toDetailResponse(savedGroup);

        List<MatchingMemberResponse> memberResponses = savedGroup.getMembers().stream()
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED && !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(matchingGroupMapper::toMemberResponse)
                .toList();
        response.setMembers(memberResponses);

        return response;
    }

    @Override
    @Transactional
    public MatchingMemberResponse joinMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to join matching group: groupId={}, userId={}", groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findDetailById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (matchingGroup.getStatus() != MatchingGroupStatus.OPEN) {
            throw new AppException(ErrorCode.MATCHING_GROUP_NOT_OPEN);
        }

        if (LocalDateTime.now().isAfter(matchingGroup.getMatchingDeadline())) {
            throw new AppException(ErrorCode.MATCHING_DEADLINE_PASSED);
        }

        long acceptedCount = matchingGroup.getMembers().stream()
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED && !Boolean.TRUE.equals(m.getIsDeleted()))
                .count();
        if (acceptedCount >= matchingGroup.getMaxSize()) {
            throw new AppException(ErrorCode.MATCHING_GROUP_FULL);
        }

        MatchingMember member = matchingMemberRepository.findByMatchingGroupAndUser(matchingGroup, currentUser)
                .map(existingMember -> {
                    if (existingMember.getStatus() == JoinStatus.ACCEPTED) {
                        throw new AppException(ErrorCode.ALREADY_MEMBER);
                    }
                    if (existingMember.getStatus() == JoinStatus.PENDING) {
                        throw new AppException(ErrorCode.JOIN_REQUEST_PENDING);
                    }
                    existingMember.setStatus(JoinStatus.PENDING);
                    existingMember.setIsDeleted(false);
                    existingMember.setRole(MatchingRole.MEMBER);
                    return existingMember;
                })
                .orElseGet(() -> {
                    MatchingMember newMember = new MatchingMember();
                    newMember.setMatchingGroup(matchingGroup);
                    newMember.setUser(currentUser);
                    newMember.setRole(MatchingRole.MEMBER);
                    newMember.setStatus(JoinStatus.PENDING);
                    return newMember;
                });

        MatchingMember savedMember = matchingMemberRepository.save(member);

        return matchingGroupMapper.toMemberResponse(savedMember);
    }

    @Override
    @Transactional
    public MatchingMemberResponse approveMember(UUID memberId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Approving member: memberId={}, requesterId={}", memberId, currentUser.getUserId());

        MatchingMember member = matchingMemberRepository.findDetailByMemberId(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

        MatchingGroup matchingGroup = member.getMatchingGroup();

        if (matchingGroup.getStatus() != MatchingGroupStatus.OPEN) {
            throw new AppException(ErrorCode.MATCHING_GROUP_NOT_OPEN);
        }

        if (LocalDateTime.now().isAfter(matchingGroup.getMatchingDeadline())) {
            throw new AppException(ErrorCode.MATCHING_DEADLINE_PASSED);
        }

        if (!matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_APPROVE_MEMBER);
        }

        if (member.getStatus() == JoinStatus.ACCEPTED) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_APPROVED);
        }
        if (member.getStatus() != JoinStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_MEMBER_STATUS);
        }

        long acceptedCount = matchingGroup.getMembers().stream()
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED && !Boolean.TRUE.equals(m.getIsDeleted()))
                .count();
        if (acceptedCount >= matchingGroup.getMaxSize()) {
            throw new AppException(ErrorCode.MATCHING_GROUP_FULL);
        }

        member.setStatus(JoinStatus.ACCEPTED);
        int newSize = (int) (acceptedCount + 1);
        matchingGroup.setCurrentSize(newSize);

        if (newSize == matchingGroup.getMaxSize()) {
            matchingGroup.setStatus(MatchingGroupStatus.FULL);
            log.info("Matching group is now FULL: groupId={}", matchingGroup.getMatchingGroupId());
        }

        matchingMemberRepository.save(member);
        matchingGroupRepository.save(matchingGroup);

        return matchingGroupMapper.toMemberResponse(member);
    }

    @Override
    @Transactional
    public MatchingMemberResponse rejectMember(UUID memberId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Rejecting member: memberId={}, requesterId={}", memberId, currentUser.getUserId());

        MatchingMember member = matchingMemberRepository.findDetailByMemberId(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

        MatchingGroup matchingGroup = member.getMatchingGroup();

        if (matchingGroup.getStatus() == MatchingGroupStatus.CLOSED || matchingGroup.getStatus() == MatchingGroupStatus.HIDDEN) {
            throw new AppException(ErrorCode.MATCHING_GROUP_NOT_OPEN);
        }

        if (LocalDateTime.now().isAfter(matchingGroup.getMatchingDeadline())) {
            throw new AppException(ErrorCode.MATCHING_DEADLINE_PASSED);
        }

        if (!matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_REJECT_MEMBER);
        }

        if (member.getStatus() == JoinStatus.REJECTED) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_REJECTED);
        }
        if (member.getStatus() != JoinStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_MEMBER_STATUS);
        }

        member.setStatus(JoinStatus.REJECTED);

        MatchingMember savedMember = matchingMemberRepository.save(member);

        return matchingGroupMapper.toMemberResponse(savedMember);
    }

    @Override
    @Transactional
    public MatchingMemberResponse leaveMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to leave matching group: groupId={}, userId={}", groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findDetailById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (matchingGroup.getStatus() == MatchingGroupStatus.CLOSED || matchingGroup.getStatus() == MatchingGroupStatus.HIDDEN) {
            throw new AppException(ErrorCode.MATCHING_GROUP_NOT_OPEN);
        }

        if (matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.OWNER_CANNOT_LEAVE);
        }

        MatchingMember member = matchingMemberRepository.findByMatchingGroupAndUser(matchingGroup, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_A_MEMBER));

        if (member.getStatus() == JoinStatus.LEFT || member.getStatus() == JoinStatus.REJECTED) {
            throw new AppException(ErrorCode.NOT_A_MEMBER);
        }

        if (member.getStatus() == JoinStatus.ACCEPTED) {
            member.setStatus(JoinStatus.LEFT);

            long acceptedCount = matchingGroup.getMembers().stream()
                    .filter(m -> m.getStatus() == JoinStatus.ACCEPTED && !Boolean.TRUE.equals(m.getIsDeleted()))
                    .count();
            int newSize = (int) (acceptedCount - 1);
            if (newSize < 1) newSize = 1;
            matchingGroup.setCurrentSize(newSize);

            if (matchingGroup.getStatus() == MatchingGroupStatus.FULL) {
                matchingGroup.setStatus(MatchingGroupStatus.OPEN);
                log.info("Matching group is reopened (OPEN) because a member left: groupId={}", groupId);
            }

            matchingGroupRepository.save(matchingGroup);
        } else if (member.getStatus() == JoinStatus.PENDING) {
            member.setStatus(JoinStatus.LEFT);
        }

        MatchingMember savedMember = matchingMemberRepository.save(member);

        return matchingGroupMapper.toMemberResponse(savedMember);
    }

    @Override
    @Transactional
    public void disbandMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to disband matching group: groupId={}, userId={}", groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findDetailById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (!matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_DISBAND_GROUP);
        }

        matchingGroup.setIsDeleted(true);
        matchingGroup.setStatus(MatchingGroupStatus.CLOSED);

        if (matchingGroup.getMembers() != null) {
            matchingGroup.getMembers().forEach(member -> member.setIsDeleted(true));
        }

        matchingGroupRepository.save(matchingGroup);
        log.info("Matching group disbanded successfully: groupId={}", groupId);
    }
}
