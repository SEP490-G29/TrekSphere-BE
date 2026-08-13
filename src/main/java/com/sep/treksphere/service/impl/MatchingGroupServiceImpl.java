package com.sep.treksphere.service.impl;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.request.MatchingJoinRequestFilter;
import com.sep.treksphere.dto.request.MyMatchingJoinRequestFilter;
import com.sep.treksphere.dto.request.OwnedMatchingGroupFilterRequest;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.dto.response.MatchingGroupResponse;
import com.sep.treksphere.dto.response.MatchingMemberResponse;
import com.sep.treksphere.dto.response.MyMatchingJoinRequestResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.MatchingMember;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import com.sep.treksphere.enums.matching.MatchingRole;
import com.sep.treksphere.enums.tour.TourStatus;
import com.sep.treksphere.enums.user.UserStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.MatchingGroupMapper;
import com.sep.treksphere.repository.MatchingGroupRepository;
import com.sep.treksphere.repository.MatchingMemberRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.UserRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingGroupServiceImpl implements MatchingGroupService {

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final MatchingGroupMapper matchingGroupMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter) {
        String keyword = filter.getKeyword() == null
                ? ""
                : filter.getKeyword().trim().toLowerCase(Locale.ROOT);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        log.info("Fetching available matching groups with filters: tourId={}, targetDate={}, keyword={}",
                filter.getTourId(), filter.getTargetDate(), keyword);

        Page<MatchingGroup> groups = matchingGroupRepository.findAvailableMatchingGroups(
                MatchingGroupStatus.OPEN,
                TourStatus.APPROVED,
                filter.getTourId(),
                filter.getTargetDate(),
                keyword,
                today,
                now,
                filter.getPageable()
        );

        return PaginationUtils.toPaginationResponse(groups.map(matchingGroupMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MatchingGroupResponse> getOwnedMatchingGroups(
            OwnedMatchingGroupFilterRequest filter,
            CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getUserId();
        String keyword = filter.getKeyword() == null
                ? ""
                : filter.getKeyword().trim().toLowerCase(Locale.ROOT);

        log.info("Fetching owned or joined matching groups: userId={}, status={}, keyword={}",
                userId, filter.getStatus(), keyword);

        Page<MatchingGroup> groups = matchingGroupRepository.findOwnedOrJoinedGroups(
                userId,
                MatchingRole.MEMBER,
                JoinStatus.ACCEPTED,
                filter.getStatus(),
                keyword,
                filter.getPageable()
        );

        return PaginationUtils.toPaginationResponse(groups.map(matchingGroupMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public MatchingGroupDetailResponse getMatchingGroupById(UUID id, CustomUserDetails userDetails) {
        log.info("Fetching matching group detail: id={}", id);

        MatchingGroup matchingGroup = matchingGroupRepository.findPublicDetailById(
                        id,
                        Set.of(MatchingGroupStatus.OPEN, MatchingGroupStatus.FULL),
                        TourStatus.APPROVED
                )
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        MatchingGroupDetailResponse response = matchingGroupMapper.toDetailResponse(matchingGroup);

        Set<UUID> usersInConversation = new HashSet<>();
        if (matchingGroup.getConversation() != null && !Boolean.TRUE.equals(matchingGroup.getConversation().getIsDeleted())) {
            matchingGroup.getConversation().getParticipants().forEach(p -> usersInConversation.add(p.getUserId()));
        }

        List<MatchingMemberResponse> acceptedMembers = matchingGroup.getMembers().stream()
                .filter(member -> member.getStatus() == JoinStatus.ACCEPTED
                        && !Boolean.TRUE.equals(member.getIsDeleted()))
                .map(member -> {
                    MatchingMemberResponse memResponse = matchingGroupMapper.toMemberResponse(member);
                    memResponse.setIsInConversation(usersInConversation.contains(member.getUser().getUserId()));
                    return memResponse;
                })
                .toList();

        response.setMembers(acceptedMembers);

        UUID viewerId = userDetails == null ? null : userDetails.getUser().getUserId();
        boolean isOwner = viewerId != null && matchingGroup.getOwner().getUserId().equals(viewerId);
        MatchingMember viewerMembership = viewerId == null
                ? null
                : matchingGroup.getMembers().stream()
                        .filter(member -> member.getUser().getUserId().equals(viewerId)
                                && !Boolean.TRUE.equals(member.getIsDeleted()))
                        .findFirst()
                        .orElse(null);

        JoinStatus membershipStatus = viewerMembership == null ? null : viewerMembership.getStatus();
        boolean hasActiveMembership = membershipStatus == JoinStatus.PENDING
                || membershipStatus == JoinStatus.ACCEPTED;
        boolean groupIsJoinable = matchingGroup.getStatus() == MatchingGroupStatus.OPEN
                && matchingGroup.getCurrentSize() < matchingGroup.getMaxSize()
                && matchingGroup.getMatchingDeadline().isAfter(LocalDateTime.now())
                && matchingGroup.getTargetDate().isAfter(LocalDate.now());

        response.setIsOwner(isOwner);
        response.setMyMembershipStatus(membershipStatus);
        response.setCanJoin(viewerId != null && !isOwner && !hasActiveMembership && groupIsJoinable);
        response.setCanLeave(viewerId != null && !isOwner && hasActiveMembership);
        
        boolean isInConversation = false;
        if (viewerId != null && matchingGroup.getConversation() != null && !Boolean.TRUE.equals(matchingGroup.getConversation().getIsDeleted())) {
            isInConversation = matchingGroup.getConversation().getParticipants().stream()
                    .anyMatch(p -> p.getUserId().equals(viewerId));
        }
        response.setIsInConversation(isInConversation);

        return response;
    }

    @Override
    @Transactional
    public MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, CustomUserDetails userDetails) {
        User currentUser = userRepository.findByIdForUpdate(userDetails.getUser().getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        log.info("Creating matching group: ownerId={}, groupName={}", currentUser.getUserId(), request.getGroupName());

        String normalizedGroupName = request.getGroupName().trim();
        if (normalizedGroupName.length() < 3 || normalizedGroupName.length() > 100) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    MessageConstant.MATCHING_GROUP_NAME_SIZE);
        }
        String normalizedDescription = request.getDescription() == null ? null : request.getDescription().trim();
        if (normalizedDescription != null && normalizedDescription.isEmpty()) {
            normalizedDescription = null;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(request.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (tour.getStatus() != TourStatus.APPROVED) {
            throw new AppException(ErrorCode.MATCHING_TOUR_NOT_APPROVED);
        }

        if (!request.getTargetDate().isAfter(today)) {
            throw new AppException(ErrorCode.INVALID_TARGET_DATE);
        }

        if (!request.getMatchingDeadline().isAfter(now)) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
        if (request.getMatchingDeadline().toLocalDate().isAfter(request.getTargetDate())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        if (tour.getMaxCapacity() != null && request.getMaxSize() > tour.getMaxCapacity()) {
            throw new AppException(ErrorCode.MATCHING_GROUP_SIZE_EXCEEDS_TOUR_CAPACITY);
        }

        boolean hasActiveGroupForTour = matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        currentUser,
                        tour,
                        Set.of(MatchingGroupStatus.OPEN, MatchingGroupStatus.FULL),
                        now,
                        today
                );
        if (hasActiveGroupForTour) {
            throw new AppException(ErrorCode.ALREADY_HAS_ACTIVE_GROUP);
        }

        MatchingGroup matchingGroup = new MatchingGroup();
        matchingGroup.setTour(tour);
        matchingGroup.setOwner(currentUser);
        matchingGroup.setGroupName(normalizedGroupName);
        matchingGroup.setDescription(normalizedDescription);
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
        User currentUser = userRepository.findByIdForUpdate(userDetails.getUser().getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        log.info("Request to join matching group: groupId={}, userId={}", groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findDetailById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        Tour tour = matchingGroup.getTour();
        if (Boolean.TRUE.equals(tour.getIsDeleted()) || tour.getStatus() != TourStatus.APPROVED) {
            throw new AppException(ErrorCode.MATCHING_TOUR_NOT_AVAILABLE);
        }

        if (matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.MATCHING_OWNER_CANNOT_JOIN);
        }

        if (matchingGroup.getStatus() != MatchingGroupStatus.OPEN) {
            throw new AppException(ErrorCode.MATCHING_GROUP_NOT_OPEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!matchingGroup.getMatchingDeadline().isAfter(now)) {
            throw new AppException(ErrorCode.MATCHING_DEADLINE_PASSED);
        }

        if (!matchingGroup.getTargetDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.MATCHING_TARGET_DATE_PASSED);
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
    @Transactional(readOnly = true)
    public PaginationResponse<MatchingMemberResponse> getJoinRequests(
            UUID groupId,
            MatchingJoinRequestFilter filter,
            CustomUserDetails userDetails
    ) {
        JoinStatus status = filter.getStatus() == null ? JoinStatus.PENDING : filter.getStatus();
        if (status != JoinStatus.PENDING && status != JoinStatus.REJECTED) {
            throw new AppException(ErrorCode.INVALID_JOIN_REQUEST_FILTER_STATUS);
        }
        if (filter.getPage() < 0 || filter.getSize() < 1 || filter.getSize() > 50) {
            throw new AppException(ErrorCode.INVALID_JOIN_REQUEST_PAGINATION);
        }

        User currentUser = userDetails.getUser();
        log.info("Fetching matching group join requests: groupId={}, status={}, requesterId={}",
                groupId, status, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findWithOwnerById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (!matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_VIEW_JOIN_REQUESTS);
        }

        Page<MatchingMemberResponse> joinRequests = matchingMemberRepository
                .findJoinRequests(groupId, status, MatchingRole.MEMBER, filter.getPageable())
                .map(matchingGroupMapper::toMemberResponse);

        return PaginationUtils.toPaginationResponse(joinRequests);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MyMatchingJoinRequestResponse> getMyJoinRequests(
            MyMatchingJoinRequestFilter filter,
            CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getUserId();
        JoinStatus status = filter.getStatus();

        log.info("Fetching current Trekker matching join requests: userId={}, status={}", userId, status);

        Page<MyMatchingJoinRequestResponse> requests = matchingMemberRepository.findMyJoinRequests(
                        userId,
                        MatchingRole.MEMBER,
                        status,
                        filter.getPageable()
                )
                .map(member -> {
                    MyMatchingJoinRequestResponse response =
                            matchingGroupMapper.toMyJoinRequestResponse(member);
                    response.setCanCancel(member.getStatus() == JoinStatus.PENDING);
                    return response;
                });

        return PaginationUtils.toPaginationResponse(requests);
    }

    @Override
    @Transactional
    public MatchingMemberResponse approveMember(
            UUID groupId,
            UUID memberId,
            CustomUserDetails userDetails
    ) {
        User currentUser = userDetails.getUser();
        log.info("Approving matching group join request: groupId={}, memberId={}, requesterId={}",
                groupId, memberId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (!matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_APPROVE_MEMBER);
        }

        if (matchingGroup.getStatus() != MatchingGroupStatus.OPEN) {
            throw new AppException(ErrorCode.MATCHING_GROUP_NOT_OPEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!matchingGroup.getMatchingDeadline().isAfter(now)) {
            throw new AppException(ErrorCode.MATCHING_DEADLINE_PASSED);
        }

        if (!matchingGroup.getTargetDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.MATCHING_TARGET_DATE_PASSED);
        }

        Tour tour = matchingGroup.getTour();
        if (Boolean.TRUE.equals(tour.getIsDeleted()) || tour.getStatus() != TourStatus.APPROVED) {
            throw new AppException(ErrorCode.MATCHING_TOUR_NOT_AVAILABLE);
        }

        MatchingMember member = matchingMemberRepository.findJoinRequestByIdAndGroupId(
                        memberId,
                        groupId,
                        MatchingRole.MEMBER
                )
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

        if (member.getStatus() == JoinStatus.ACCEPTED) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_APPROVED);
        }
        if (member.getStatus() != JoinStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_MEMBER_STATUS);
        }

        long acceptedCount = matchingMemberRepository
                .countActiveMembersByGroupIdAndStatus(
                        groupId,
                        JoinStatus.ACCEPTED
                );
        if (acceptedCount >= matchingGroup.getMaxSize()) {
            throw new AppException(ErrorCode.MATCHING_GROUP_FULL);
        }

        member.setStatus(JoinStatus.ACCEPTED);
        int newSize = Math.toIntExact(acceptedCount + 1);
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
    public MatchingMemberResponse rejectMember(
            UUID groupId,
            UUID memberId,
            CustomUserDetails userDetails
    ) {
        User currentUser = userDetails.getUser();
        log.info("Rejecting matching group join request: groupId={}, memberId={}, requesterId={}",
                groupId, memberId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findWithOwnerById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (!matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_REJECT_MEMBER);
        }

        MatchingMember member = matchingMemberRepository.findJoinRequestByIdAndGroupId(
                        memberId,
                        groupId,
                        MatchingRole.MEMBER
                )
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

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
    public MatchingMemberResponse cancelJoinRequest(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to cancel matching group join request: groupId={}, userId={}",
                groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        MatchingMember member = matchingMemberRepository.findByMatchingGroupAndUser(matchingGroup, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.NO_PENDING_JOIN_REQUEST));

        if (Boolean.TRUE.equals(member.getIsDeleted())
                || member.getRole() != MatchingRole.MEMBER
                || member.getStatus() != JoinStatus.PENDING) {
            throw new AppException(ErrorCode.NO_PENDING_JOIN_REQUEST);
        }

        member.setStatus(JoinStatus.CANCELLED);
        MatchingMember savedMember = matchingMemberRepository.save(member);
        return matchingGroupMapper.toMemberResponse(savedMember);
    }

    @Override
    @Transactional
    public MatchingMemberResponse leaveMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to leave matching group: groupId={}, userId={}",
                groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.OWNER_CANNOT_LEAVE);
        }

        MatchingMember member = matchingMemberRepository.findByMatchingGroupAndUser(matchingGroup, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_A_MEMBER));

        if (Boolean.TRUE.equals(member.getIsDeleted())
                || member.getRole() != MatchingRole.MEMBER
                || member.getStatus() != JoinStatus.ACCEPTED) {
            throw new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
        }

        long acceptedCount = matchingMemberRepository
                .countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED);

        member.setStatus(JoinStatus.LEFT);

        int newSize = Math.max(Math.toIntExact(acceptedCount) - 1, 1);
        matchingGroup.setCurrentSize(newSize);

        Tour tour = matchingGroup.getTour();
        boolean canReopen = matchingGroup.getStatus() == MatchingGroupStatus.FULL
                && newSize < matchingGroup.getMaxSize()
                && matchingGroup.getMatchingDeadline().isAfter(LocalDateTime.now())
                && matchingGroup.getTargetDate().isAfter(LocalDate.now())
                && !Boolean.TRUE.equals(tour.getIsDeleted())
                && tour.getStatus() == TourStatus.APPROVED;

        if (canReopen) {
            matchingGroup.setStatus(MatchingGroupStatus.OPEN);
            log.info("Matching group is reopened (OPEN) because a member left: groupId={}", groupId);
        }

        matchingGroupRepository.save(matchingGroup);

        MatchingMember savedMember = matchingMemberRepository.save(member);

        return matchingGroupMapper.toMemberResponse(savedMember);
    }

    @Override
    @Transactional
    public void disbandMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to disband matching group: groupId={}, userId={}", groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (!matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_DISBAND_GROUP);
        }

        if (matchingGroup.getStatus() != MatchingGroupStatus.OPEN
                && matchingGroup.getStatus() != MatchingGroupStatus.FULL) {
            throw new AppException(ErrorCode.MATCHING_GROUP_CANNOT_BE_DISBANDED);
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        String deletedBy = currentUser.getUserId().toString();
        matchingGroup.setIsDeleted(true);
        matchingGroup.setStatus(MatchingGroupStatus.CLOSED);
        matchingGroup.setDeletedAt(deletedAt);
        matchingGroup.setDeletedBy(deletedBy);

        if (matchingGroup.getMembers() != null) {
            matchingGroup.getMembers().forEach(member -> {
                member.setIsDeleted(true);
                member.setDeletedAt(deletedAt);
                member.setDeletedBy(deletedBy);
            });
        }

        matchingGroupRepository.save(matchingGroup);
        log.info("Matching group disbanded successfully: groupId={}", groupId);
    }
}
