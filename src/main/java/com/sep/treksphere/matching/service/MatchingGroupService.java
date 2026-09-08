package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.common.util.PaginationUtils;
import com.sep.treksphere.matching.dto.request.CustomJourneyCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.request.MatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.request.MyMatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.request.MyMatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.matching.dto.response.MatchingGroupResponse;
import com.sep.treksphere.matching.dto.response.MatchingMemberResponse;
import com.sep.treksphere.matching.dto.response.MyMatchingJoinRequestResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.MatchingGroupMapper;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import com.sep.treksphere.vendor.VendorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingGroupService {

    private static final Set<MatchingGroupStatus> ACTIVE_GROUP_STATUSES =
            Set.of(MatchingGroupStatus.OPEN, MatchingGroupStatus.FULL);

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final GroupTripRepository groupTripRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final MatchingGroupMapper matchingGroupMapper;

    @Transactional(readOnly = true)
    public PaginationResponse<MatchingGroupResponse> getMatchingGroups(MatchingGroupFilterRequest filter) {
        String keyword = filter.getKeyword() == null
                ? ""
                : filter.getKeyword().trim().toLowerCase(Locale.ROOT);

        String sourceType = filter.getSourceType() == null
                ? null
                : filter.getSourceType().name();

        String difficulty = filter.getDifficulty() == null
                ? null
                : filter.getDifficulty().name();

        String location = filter.getLocation() == null || filter.getLocation().isBlank()
                ? null
                : filter.getLocation().trim();

        Boolean availableSlotsOnly = filter.getAvailableSlotsOnly() != null ? filter.getAvailableSlotsOnly() : true;

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        log.info("Fetching available matching groups with filters: sourceType={}, tourId={}, difficulty={}, location={}, targetDate={}, targetDateFrom={}, targetDateTo={}, availableSlotsOnly={}, keyword={}",
                sourceType, filter.getTourId(), difficulty, location, filter.getTargetDate(), filter.getTargetDateFrom(), filter.getTargetDateTo(), availableSlotsOnly, keyword);

        Page<MatchingGroup> groups = matchingGroupRepository.findAvailableMatchingGroups(
                MatchingGroupStatus.OPEN,
                TourStatus.PUBLISHED,
                VendorStatus.ACTIVE,
                sourceType,
                filter.getTourId(),
                filter.getTargetDate(),
                filter.getTargetDateFrom(),
                filter.getTargetDateTo(),
                difficulty,
                location,
                availableSlotsOnly,
                keyword,
                today,
                now,
                filter.getPageable()
        );

        return PaginationUtils.toPaginationResponse(groups.map(matchingGroupMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PaginationResponse<MatchingGroupResponse> getMyMatchingGroups(
            MyMatchingGroupFilterRequest filter,
            CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getUserId();
        String keyword = filter.getKeyword() == null
                ? ""
                : filter.getKeyword().trim().toLowerCase(Locale.ROOT);

        log.info("Fetching owned or joined matching groups: userId={}, role={}, status={}, keyword={}",
                userId, filter.getRole(), filter.getStatus(), keyword);

        Page<MatchingGroup> groups = matchingGroupRepository.findOwnedOrJoinedGroups(
                userId,
                MatchingRole.MEMBER,
                JoinStatus.ACCEPTED,
                filter.getRole(),
                filter.getStatus(),
                keyword,
                filter.getPageable()
        );

        return PaginationUtils.toPaginationResponse(groups.map(group -> {
            MatchingGroupResponse response = matchingGroupMapper.toResponse(group);
            boolean isOwner = group.getOwner() != null && userId.equals(group.getOwner().getUserId());
            response.setIsOwner(isOwner);
            response.setMyRole(isOwner ? MatchingRole.LEADER : MatchingRole.MEMBER);
            return response;
        }));
    }

    @Transactional(readOnly = true)
    public MatchingGroupDetailResponse getMatchingGroupById(UUID id, CustomUserDetails userDetails) {
        log.info("Fetching matching group detail: id={}", id);

        MatchingGroup matchingGroup = matchingGroupRepository.findPublicDetailById(
                        id,
                        Set.of(MatchingGroupStatus.OPEN, MatchingGroupStatus.FULL),
                        TourStatus.PUBLISHED,
                        VendorStatus.ACTIVE
                )
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        MatchingGroupDetailResponse response = matchingGroupMapper.toDetailResponse(matchingGroup);

        Set<UUID> usersInConversation = new HashSet<>();
        if (matchingGroup.getConversation() != null && !Boolean.TRUE.equals(matchingGroup.getConversation().getIsDeleted())) {
            matchingGroup.getConversation().getParticipants().forEach(p -> usersInConversation.add(p.getUserId()));
        }

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
        boolean isAcceptedMember = isOwner || membershipStatus == JoinStatus.ACCEPTED;

        // Chỉ Accepted Member và Leader mới xem được danh sách thành viên (theo Artifact B Permission Matrix)
        if (isAcceptedMember) {
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
        } else {
            response.setMembers(Collections.emptyList());
        }

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

    @Transactional
    public MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, UUID userId) {
        User currentUser = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        log.info("Creating matching group: ownerId={}, groupName={}", currentUser.getUserId(), request.getGroupName());

        String normalizedGroupName = request.getGroupName().trim();
        String normalizedDescription = normalizeNullableText(request.getDescription());

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        validateSource(request);

        if (!request.getTargetDate().isAfter(today)) {
            throw new AppException(ErrorCode.INVALID_TARGET_DATE);
        }

        if (!request.getMatchingDeadline().isAfter(now)) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
        if (request.getMatchingDeadline().toLocalDate().isAfter(request.getTargetDate())) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        if (!request.getScheduledStartAt().isAfter(now)) {
            throw new AppException(ErrorCode.INVALID_SCHEDULED_START);
        }

        Tour tour = resolveTourSource(request, currentUser, now, today);
        CustomJourney customJourney = resolveCustomJourneySource(request, currentUser, normalizedGroupName);

        MatchingGroup matchingGroup = matchingGroupMapper.toEntity(request);
        matchingGroup.setTour(tour);
        matchingGroup.setOwner(currentUser);
        matchingGroup.setGroupName(normalizedGroupName);
        matchingGroup.setDescription(normalizedDescription);

        if (customJourney != null) {
            customJourney.setMatchingGroup(matchingGroup);
            matchingGroup.setCustomJourney(customJourney);
        }

        MatchingMember leaderMembership = new MatchingMember();
        leaderMembership.setMatchingGroup(matchingGroup);
        leaderMembership.setUser(currentUser);
        leaderMembership.setRole(MatchingRole.LEADER);
        leaderMembership.setStatus(JoinStatus.ACCEPTED);

        matchingGroup.getMembers().add(leaderMembership);

        MatchingGroup savedGroup = matchingGroupRepository.save(matchingGroup);

        GroupTrip groupTrip = new GroupTrip();
        groupTrip.setMatchingGroup(savedGroup);
        groupTrip.setStatus(GroupTripStatus.PLANNED);
        groupTrip.setScheduledStartAt(request.getScheduledStartAt());
        groupTripRepository.save(groupTrip);

        MatchingGroupDetailResponse response = matchingGroupMapper.toDetailResponse(savedGroup);

        MatchingMemberResponse leaderResponse = matchingGroupMapper.toMemberResponse(leaderMembership);
        leaderResponse.setIsInConversation(false);
        response.setMembers(List.of(leaderResponse));

        response.setIsOwner(true);
        response.setMyMembershipStatus(JoinStatus.ACCEPTED);
        response.setCanJoin(false);
        response.setCanLeave(false);
        response.setHasConversation(false);
        response.setIsInConversation(false);

        return response;
    }

    private void validateSource(MatchingGroupCreateRequest request) {
        boolean hasTour = request.getTourId() != null;
        boolean hasCustomJourney = request.getCustomJourney() != null;

        if (request.getSourceType() == MatchingGroupSourceType.TOUR && hasTour && !hasCustomJourney) {
            return;
        }
        if (request.getSourceType() == MatchingGroupSourceType.CUSTOM_JOURNEY && !hasTour && hasCustomJourney) {
            return;
        }
        throw new AppException(ErrorCode.MATCHING_GROUP_SOURCE_INVALID);
    }

    private Tour resolveTourSource(
            MatchingGroupCreateRequest request,
            User currentUser,
            LocalDateTime now,
            LocalDate today
    ) {
        if (request.getSourceType() != MatchingGroupSourceType.TOUR) {
            return null;
        }

        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(request.getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (tour.getStatus() != TourStatus.PUBLISHED
                || tour.getVendor() == null
                || tour.getVendor().getStatus() != VendorStatus.ACTIVE
                || Boolean.TRUE.equals(tour.getVendor().getIsDeleted())) {
            throw new AppException(ErrorCode.MATCHING_TOUR_NOT_APPROVED);
        }

        if ((tour.getMinCapacity() != null && request.getMaxSize() < tour.getMinCapacity())
                || (tour.getMaxCapacity() != null && request.getMaxSize() > tour.getMaxCapacity())) {
            throw new AppException(ErrorCode.MATCHING_GROUP_SIZE_EXCEEDS_TOUR_CAPACITY);
        }

        boolean duplicate = matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        currentUser,
                        tour,
                        ACTIVE_GROUP_STATUSES,
                        now,
                        today
                );
        if (duplicate) {
            throw new AppException(ErrorCode.ALREADY_HAS_ACTIVE_GROUP);
        }
        return tour;
    }

    private CustomJourney resolveCustomJourneySource(
            MatchingGroupCreateRequest request,
            User currentUser,
            String normalizedGroupName
    ) {
        if (request.getSourceType() != MatchingGroupSourceType.CUSTOM_JOURNEY) {
            return null;
        }

        CustomJourneyCreateRequest customJourneyRequest = request.getCustomJourney();
        if (customJourneyRequest.getEndDate().isBefore(customJourneyRequest.getStartDate())) {
            throw new AppException(ErrorCode.CUSTOM_JOURNEY_DATE_INVALID);
        }
        if (!customJourneyRequest.getStartDate().equals(request.getTargetDate())) {
            throw new AppException(ErrorCode.CUSTOM_JOURNEY_TARGET_DATE_MISMATCH);
        }

        String normalizedTitle = customJourneyRequest.getTitle().trim();

        boolean duplicate = matchingGroupRepository
                .existsByOwnerAndTourIsNullAndGroupNameIgnoreCaseAndTargetDateAndIsDeletedFalse(
                        currentUser,
                        normalizedGroupName,
                        request.getTargetDate()
                );
        if (duplicate) {
            throw new AppException(ErrorCode.DUPLICATE_MATCHING_GROUP);
        }

        CustomJourney customJourney = matchingGroupMapper.toEntity(customJourneyRequest);
        customJourney.setTitle(normalizedTitle);
        customJourney.setDescription(normalizeNullableText(customJourneyRequest.getDescription()));
        return customJourney;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Transactional
    public MatchingMemberResponse joinMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userRepository.findByIdForUpdate(userDetails.getUser().getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        log.info("Request to join matching group: groupId={}, userId={}", groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.MATCHING_OWNER_CANNOT_JOIN);
        }

        validateGroupOpenAndActive(matchingGroup);

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

        validateGroupOwner(matchingGroup, currentUser, ErrorCode.UNAUTHORIZED_VIEW_JOIN_REQUESTS);

        Page<MatchingMemberResponse> joinRequests = matchingMemberRepository
                .findJoinRequests(groupId, status, MatchingRole.MEMBER, filter.getPageable())
                .map(matchingGroupMapper::toMemberResponse);

        return PaginationUtils.toPaginationResponse(joinRequests);
    }

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

        validateGroupOwner(matchingGroup, currentUser, ErrorCode.UNAUTHORIZED_APPROVE_MEMBER);
        validateGroupOpenAndActive(matchingGroup);

        MatchingMember member = matchingMemberRepository.findDetailByMemberId(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

        if (!member.getMatchingGroup().getMatchingGroupId().equals(groupId)) {
            throw new AppException(ErrorCode.CROSS_GROUP_ACTION_NOT_ALLOWED);
        }

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

    @Transactional
    public MatchingMemberResponse rejectMember(
            UUID groupId,
            UUID memberId,
            CustomUserDetails userDetails
    ) {
        User currentUser = userDetails.getUser();
        log.info("Rejecting matching group join request: groupId={}, memberId={}, requesterId={}",
                groupId, memberId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupOwner(matchingGroup, currentUser, ErrorCode.UNAUTHORIZED_REJECT_MEMBER);

        MatchingMember member = matchingMemberRepository.findDetailByMemberId(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

        if (!member.getMatchingGroup().getMatchingGroupId().equals(groupId)) {
            throw new AppException(ErrorCode.CROSS_GROUP_ACTION_NOT_ALLOWED);
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

        member.setStatus(JoinStatus.WITHDRAWN);
        member.setWithdrawnAt(java.time.LocalDateTime.now());
        MatchingMember savedMember = matchingMemberRepository.save(member);
        return matchingGroupMapper.toMemberResponse(savedMember);
    }

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
                && tour.getStatus() == TourStatus.PUBLISHED
                && tour.getVendor().getStatus() == com.sep.treksphere.vendor.VendorStatus.ACTIVE;

        if (canReopen) {
            matchingGroup.setStatus(MatchingGroupStatus.OPEN);
            log.info("Matching group is reopened (OPEN) because a member left: groupId={}", groupId);
        }

        matchingGroupRepository.save(matchingGroup);

        MatchingMember savedMember = matchingMemberRepository.save(member);

        return matchingGroupMapper.toMemberResponse(savedMember);
    }

    @Transactional
    public void disbandMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to disband matching group: groupId={}, userId={}", groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupOwner(matchingGroup, currentUser, ErrorCode.UNAUTHORIZED_DISBAND_GROUP);

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

    private void validateGroupOpenAndActive(MatchingGroup matchingGroup) {
        Tour tour = matchingGroup.getTour();
        if (tour != null && (Boolean.TRUE.equals(tour.getIsDeleted())
                || tour.getStatus() != TourStatus.PUBLISHED
                || tour.getVendor().getStatus() != com.sep.treksphere.vendor.VendorStatus.ACTIVE
                || Boolean.TRUE.equals(tour.getVendor().getIsDeleted()))) {
            throw new AppException(ErrorCode.MATCHING_TOUR_NOT_AVAILABLE);
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
    }

    private void validateGroupOwner(MatchingGroup matchingGroup, User currentUser, ErrorCode errorCode) {
        if (!matchingGroup.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(errorCode);
        }
    }
}
