package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.common.util.PaginationUtils;
import com.sep.treksphere.matching.dto.request.CustomJourneyCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupUpdateRequest;
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
import com.sep.treksphere.matching.entity.GroupJoinApplication;
import com.sep.treksphere.matching.enums.JoinApplicationStatus;
import com.sep.treksphere.matching.repository.GroupJoinApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sep.treksphere.matching.dto.request.GroupApplicationRequest;
import com.sep.treksphere.matching.event.GroupApplicationSubmittedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import com.sep.treksphere.matching.event.GroupApplicationDecidedEvent;
import com.sep.treksphere.matching.event.GroupMembershipActivatedEvent;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    private final GroupJoinApplicationRepository groupJoinApplicationRepository;
    private final GroupTripRepository groupTripRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final MatchingGroupMapper matchingGroupMapper;
    private final ApplicationEventPublisher eventPublisher;


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

        MatchingGroup matchingGroup = matchingGroupRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        UUID viewerId = userDetails == null ? null : userDetails.getUser().getUserId();
        boolean isOwner = viewerId != null && matchingGroup.getOwner() != null && matchingGroup.getOwner().getUserId().equals(viewerId);
        MatchingMember viewerMembership = (viewerId == null || matchingGroup.getMembers() == null)
                ? null
                : matchingGroup.getMembers().stream()
                        .filter(member -> member.getUser().getUserId().equals(viewerId)
                                && !Boolean.TRUE.equals(member.getIsDeleted()))
                        .findFirst()
                        .orElse(null);

        JoinStatus membershipStatus = viewerMembership == null ? null : viewerMembership.getStatus();
        if (membershipStatus == null && viewerId != null && !isOwner) {
            boolean hasPending = groupJoinApplicationRepository
                    .existsByMatchingGroup_MatchingGroupIdAndApplicant_UserIdAndStatusAndIsDeletedFalse(
                            id, viewerId, JoinApplicationStatus.PENDING
                    );
            if (hasPending) {
                membershipStatus = JoinStatus.PENDING;
            }
        }
        boolean isAcceptedMember = isOwner || membershipStatus == JoinStatus.ACCEPTED;


        // Nếu không phải Leader/Member của nhóm, chỉ cho phép xem nếu nhóm ở trạng thái public (OPEN/FULL) và Tour/Vendor khả dụng
        if (!isAcceptedMember) {
            if (matchingGroup.getStatus() != MatchingGroupStatus.OPEN && matchingGroup.getStatus() != MatchingGroupStatus.FULL) {
                throw new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND);
            }
            Tour tour = matchingGroup.getTour();
            if (tour != null && (Boolean.TRUE.equals(tour.getIsDeleted())
                    || tour.getStatus() != TourStatus.PUBLISHED
                    || tour.getVendor() == null
                    || tour.getVendor().getStatus() != VendorStatus.ACTIVE
                    || Boolean.TRUE.equals(tour.getVendor().getIsDeleted()))) {
                throw new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND);
            }
        }

        MatchingGroupDetailResponse response = matchingGroupMapper.toDetailResponse(matchingGroup);

        Set<UUID> usersInConversation = new HashSet<>();
        if (matchingGroup.getConversation() != null && !Boolean.TRUE.equals(matchingGroup.getConversation().getIsDeleted())) {
            matchingGroup.getConversation().getParticipants().forEach(p -> usersInConversation.add(p.getUserId()));
        }

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
    public MatchingMemberResponse submitApplication(
            UUID groupId,
            GroupApplicationRequest request,
            CustomUserDetails userDetails
    ) {
        User currentUser = userRepository.findByIdForUpdate(userDetails.getUser().getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        UUID userId = currentUser.getUserId();
        log.info("Request to submit application to matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (matchingGroup.getOwner().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.MATCHING_OWNER_CANNOT_JOIN);
        }

        validateGroupOpenAndActive(matchingGroup);

        // Check if user is already an active member of this group
        boolean alreadyActiveMember = matchingMemberRepository
                .existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                        groupId, userId, JoinStatus.ACCEPTED
                );
        if (alreadyActiveMember) {
            throw new AppException(ErrorCode.ALREADY_MEMBER);
        }

        // Check if user already has a pending application for this group
        boolean hasPendingApp = groupJoinApplicationRepository
                .existsByMatchingGroup_MatchingGroupIdAndApplicant_UserIdAndStatusAndIsDeletedFalse(
                        groupId, userId, JoinApplicationStatus.PENDING
                );
        if (hasPendingApp) {
            throw new AppException(ErrorCode.JOIN_REQUEST_PENDING);
        }

        long acceptedCount = matchingMemberRepository.countActiveMembersByGroupIdAndStatus(
                groupId,
                JoinStatus.ACCEPTED
        );
        if (acceptedCount >= matchingGroup.getMaxSize()) {
            throw new AppException(ErrorCode.MATCHING_GROUP_FULL);
        }

        // Create a new application record (preserves history of past rejected/withdrawn applications)
        GroupJoinApplication application = new GroupJoinApplication();
        application.setMatchingGroup(matchingGroup);
        application.setApplicant(currentUser);
        application.setMessage(request != null ? request.getMessage() : null);
        application.setStatus(JoinApplicationStatus.PENDING);

        GroupJoinApplication savedApp = groupJoinApplicationRepository.save(application);

        // [DEFERRED: Tích hợp cùng Notification module sau]
        // if (eventPublisher != null) {
        //     eventPublisher.publishEvent(GroupApplicationSubmittedEvent.builder()
        //             .eventId(UUID.randomUUID())
        //             .groupId(matchingGroup.getMatchingGroupId())
        //             .matchingMemberId(savedApp.getApplicationId())
        //             .applicantUserId(userId)
        //             .groupOwnerId(matchingGroup.getOwner().getUserId())
        //             .occurredAt(LocalDateTime.now())
        //             .build());
        // }

        return matchingGroupMapper.toMemberResponse(savedApp);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<MatchingMemberResponse> getJoinRequests(
            UUID groupId,
            MatchingJoinRequestFilter filter,
            CustomUserDetails userDetails
    ) {
        JoinApplicationStatus status = filter.getStatus() == null ? JoinApplicationStatus.PENDING : filter.getStatus();
        if (status != JoinApplicationStatus.PENDING && status != JoinApplicationStatus.REJECTED) {
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

        validateGroupLeader(matchingGroup, currentUser.getUserId());

        Page<MatchingMemberResponse> joinRequests = groupJoinApplicationRepository
                .findByGroupIdAndStatus(groupId, status, filter.getPageable())
                .map(matchingGroupMapper::toMemberResponse);

        return PaginationUtils.toPaginationResponse(joinRequests);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<MyMatchingJoinRequestResponse> getMyJoinRequests(
            MyMatchingJoinRequestFilter filter,
            CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getUserId();
        JoinApplicationStatus status = filter.getStatus();

        log.info("Fetching current Trekker matching join requests: userId={}, status={}", userId, status);

        Page<MyMatchingJoinRequestResponse> requests = groupJoinApplicationRepository.findMyApplications(
                        userId,
                        status,
                        filter.getPageable()
                )
                .map(app -> {
                    MyMatchingJoinRequestResponse response =
                            matchingGroupMapper.toMyJoinRequestResponse(app);
                    boolean isPending = app.getStatus() == JoinApplicationStatus.PENDING;
                    response.setCanCancel(isPending);
                    response.setCanWithdraw(isPending);
                    return response;
                });

        return PaginationUtils.toPaginationResponse(requests);
    }

    @Transactional
    public MatchingMemberResponse approveMember(
            UUID groupId,
            UUID applicationId,
            CustomUserDetails userDetails
    ) {
        User currentUser = userDetails.getUser();
        log.info("Approving matching group join request: groupId={}, applicationId={}, requesterId={}",
                groupId, applicationId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, currentUser.getUserId());
        validateGroupOpenAndActive(matchingGroup);

        GroupJoinApplication application = groupJoinApplicationRepository.findDetailById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

        if (!application.getMatchingGroup().getMatchingGroupId().equals(groupId)) {
            throw new AppException(ErrorCode.CROSS_GROUP_ACTION_NOT_ALLOWED);
        }

        if (application.getStatus() == JoinApplicationStatus.ACCEPTED) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_APPROVED);
        }
        if (application.getStatus() != JoinApplicationStatus.PENDING) {
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

        application.setStatus(JoinApplicationStatus.ACCEPTED);
        application.setReviewedBy(currentUser);
        application.setReviewedAt(LocalDateTime.now());
        groupJoinApplicationRepository.save(application);

        // Find or create active MatchingMember record
        MatchingMember member = matchingMemberRepository.findByMatchingGroupAndUser(matchingGroup, application.getApplicant())
                .orElseGet(() -> {
                    MatchingMember newMember = new MatchingMember();
                    newMember.setMatchingGroup(matchingGroup);
                    newMember.setUser(application.getApplicant());
                    return newMember;
                });

        member.setStatus(JoinStatus.ACCEPTED);
        member.setRole(MatchingRole.MEMBER);
        member.setSourceApplication(application);
        member.setJoinedAt(LocalDateTime.now());
        member.setIsDeleted(false);

        int newSize = Math.toIntExact(acceptedCount + 1);
        matchingGroup.setCurrentSize(newSize);

        if (newSize >= matchingGroup.getMaxSize()) {
            matchingGroup.setStatus(MatchingGroupStatus.FULL);
            log.info("Matching group is now FULL: groupId={}", matchingGroup.getMatchingGroupId());
        }

        MatchingMember savedMember = matchingMemberRepository.save(member);
        matchingGroupRepository.save(matchingGroup);

        // [DEFERRED: Tích hợp cùng Notification module sau]
        // if (eventPublisher != null) {
        //     eventPublisher.publishEvent(GroupApplicationDecidedEvent.builder()
        //             .eventId(UUID.randomUUID())
        //             .groupId(groupId)
        //             .matchingMemberId(savedMember.getMatchingMemberId())
        //             .applicantUserId(savedMember.getUser().getUserId())
        //             .decidedByUserId(currentUser.getUserId())
        //             .decision(JoinStatus.ACCEPTED)
        //             .occurredAt(LocalDateTime.now())
        //             .build());
        //
        //     eventPublisher.publishEvent(GroupMembershipActivatedEvent.builder()
        //             .eventId(UUID.randomUUID())
        //             .groupId(groupId)
        //             .matchingMemberId(savedMember.getMatchingMemberId())
        //             .userId(savedMember.getUser().getUserId())
        //             .role(MatchingRole.MEMBER)
        //             .occurredAt(LocalDateTime.now())
        //             .build());
        // }

        MatchingMemberResponse response = matchingGroupMapper.toMemberResponse(savedMember);
        response.setApplicationId(application.getApplicationId());
        return response;
    }

    @Transactional
    public MatchingMemberResponse rejectMember(
            UUID groupId,
            UUID applicationId,
            CustomUserDetails userDetails
    ) {
        User currentUser = userDetails.getUser();
        log.info("Rejecting matching group join request: groupId={}, applicationId={}, requesterId={}",
                groupId, applicationId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, currentUser.getUserId());

        GroupJoinApplication application = groupJoinApplicationRepository.findDetailById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

        if (!application.getMatchingGroup().getMatchingGroupId().equals(groupId)) {
            throw new AppException(ErrorCode.CROSS_GROUP_ACTION_NOT_ALLOWED);
        }

        if (application.getStatus() == JoinApplicationStatus.REJECTED) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_REJECTED);
        }
        if (application.getStatus() != JoinApplicationStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_MEMBER_STATUS);
        }

        application.setStatus(JoinApplicationStatus.REJECTED);
        application.setReviewedBy(currentUser);
        application.setReviewedAt(LocalDateTime.now());

        GroupJoinApplication savedApp = groupJoinApplicationRepository.save(application);

        // [DEFERRED: Tích hợp cùng Notification module sau]
        // if (eventPublisher != null) {
        //     eventPublisher.publishEvent(GroupApplicationDecidedEvent.builder()
        //             .eventId(UUID.randomUUID())
        //             .groupId(groupId)
        //             .matchingMemberId(savedApp.getApplicationId())
        //             .applicantUserId(savedApp.getApplicant().getUserId())
        //             .decidedByUserId(currentUser.getUserId())
        //             .decision(JoinStatus.REJECTED)
        //             .occurredAt(LocalDateTime.now())
        //             .build());
        // }

        return matchingGroupMapper.toMemberResponse(savedApp);
    }

    @Transactional
    public MatchingMemberResponse withdrawApplication(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to withdraw matching group application: groupId={}, userId={}",
                groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        GroupJoinApplication application = groupJoinApplicationRepository
                .findByMatchingGroupAndApplicantAndStatusAndIsDeletedFalse(
                        matchingGroup, currentUser, JoinApplicationStatus.PENDING
                )
                .orElseThrow(() -> new AppException(ErrorCode.NO_PENDING_JOIN_REQUEST));

        application.setStatus(JoinApplicationStatus.WITHDRAWN);
        application.setWithdrawnAt(LocalDateTime.now());
        GroupJoinApplication savedApp = groupJoinApplicationRepository.save(application);
        return matchingGroupMapper.toMemberResponse(savedApp);
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

    @Transactional
    public MatchingGroupDetailResponse updateMatchingGroup(
            UUID groupId,
            MatchingGroupUpdateRequest request,
            CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Updating matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, userId);
        validateGroupNotInTerminalState(matchingGroup);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 1. Validate and resolve dates with 2-way sync
        LocalDate explicitTargetDate = request.getTargetDate();
        LocalDate explicitCjStartDate = (request.getCustomJourney() != null) ? request.getCustomJourney().getStartDate() : null;

        if (explicitTargetDate != null && explicitCjStartDate != null && !explicitTargetDate.equals(explicitCjStartDate)) {
            throw new AppException(ErrorCode.CUSTOM_JOURNEY_TARGET_DATE_MISMATCH);
        }

        LocalDate newTargetDate;
        if (explicitTargetDate != null) {
            newTargetDate = explicitTargetDate;
        } else if (explicitCjStartDate != null) {
            newTargetDate = explicitCjStartDate;
        } else {
            newTargetDate = matchingGroup.getTargetDate();
        }

        LocalDateTime newDeadline = request.getMatchingDeadline() != null ? request.getMatchingDeadline() : matchingGroup.getMatchingDeadline();

        if ((explicitTargetDate != null || explicitCjStartDate != null) && !newTargetDate.isAfter(today)) {
            throw new AppException(ErrorCode.INVALID_TARGET_DATE);
        }
        if (request.getMatchingDeadline() != null && !request.getMatchingDeadline().isAfter(now)) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }
        if (newDeadline.toLocalDate().isAfter(newTargetDate)) {
            throw new AppException(ErrorCode.INVALID_DEADLINE);
        }

        matchingGroup.setTargetDate(newTargetDate);
        matchingGroup.setMatchingDeadline(newDeadline);

        // 2. Validate and update text fields
        if (request.getGroupName() != null && !request.getGroupName().isBlank()) {
            matchingGroup.setGroupName(request.getGroupName().trim());
        }
        if (request.getDescription() != null) {
            matchingGroup.setDescription(normalizeNullableText(request.getDescription()));
        }

        // 3. Validate and update capacity
        if (request.getMaxSize() != null) {
            long activeCount = matchingMemberRepository.countActiveMembersByGroupIdAndStatus(
                    groupId,
                    JoinStatus.ACCEPTED
            );
            if (request.getMaxSize() < activeCount) {
                throw new AppException(ErrorCode.MATCHING_GROUP_CAPACITY_LESS_THAN_ACTIVE_MEMBERS);
            }

            Tour tour = matchingGroup.getTour();
            if (tour != null) {
                if ((tour.getMinCapacity() != null && request.getMaxSize() < tour.getMinCapacity())
                        || (tour.getMaxCapacity() != null && request.getMaxSize() > tour.getMaxCapacity())) {
                    throw new AppException(ErrorCode.MATCHING_GROUP_SIZE_EXCEEDS_TOUR_CAPACITY);
                }
            }

            matchingGroup.setMaxSize(request.getMaxSize());

            // Auto-adjust status if OPEN or FULL
            if (matchingGroup.getStatus() == MatchingGroupStatus.OPEN
                    && matchingGroup.getCurrentSize() >= matchingGroup.getMaxSize()) {
                matchingGroup.setStatus(MatchingGroupStatus.FULL);
            } else if (matchingGroup.getStatus() == MatchingGroupStatus.FULL
                    && matchingGroup.getCurrentSize() < matchingGroup.getMaxSize()
                    && newDeadline.isAfter(now)
                    && newTargetDate.isAfter(today)) {
                matchingGroup.setStatus(MatchingGroupStatus.OPEN);
            }
        }

        // 4. Validate and update Custom Journey
        if (request.getCustomJourney() != null) {
            CustomJourney customJourney = matchingGroup.getCustomJourney();
            if (customJourney == null) {
                throw new AppException(ErrorCode.MATCHING_GROUP_SOURCE_INVALID);
            }

            if (Boolean.TRUE.equals(customJourney.getIsLocked())) {
                throw new AppException(ErrorCode.JOURNEY_LOCKED);
            }

            Optional<GroupTrip> tripOpt = groupTripRepository.findByMatchingGroup(matchingGroup);
            if (tripOpt.isPresent() && tripOpt.get().getStatus() != GroupTripStatus.PLANNED) {
                throw new AppException(ErrorCode.JOURNEY_LOCKED);
            }

            CustomJourneyUpdateRequest cjReq = request.getCustomJourney();
            LocalDate start = newTargetDate;
            LocalDate explicitEnd = cjReq.getEndDate();

            if (explicitEnd != null) {
                if (explicitEnd.isBefore(start)) {
                    throw new AppException(ErrorCode.CUSTOM_JOURNEY_DATE_INVALID);
                }
                customJourney.setEndDate(explicitEnd);
            } else if (customJourney.getStartDate() != null && customJourney.getEndDate() != null) {
                long duration = java.time.temporal.ChronoUnit.DAYS.between(customJourney.getStartDate(), customJourney.getEndDate());
                if (customJourney.getEndDate().isBefore(start)) {
                    customJourney.setEndDate(start.plusDays(Math.max(0, duration)));
                }
            }

            customJourney.setStartDate(start);

            if (cjReq.getTitle() != null && !cjReq.getTitle().isBlank()) {
                customJourney.setTitle(cjReq.getTitle().trim());
            }
            if (cjReq.getDescription() != null) {
                customJourney.setDescription(normalizeNullableText(cjReq.getDescription()));
            }
            if (cjReq.getDifficulty() != null) {
                customJourney.setDifficulty(cjReq.getDifficulty());
            }
        } else if (matchingGroup.getCustomJourney() != null && explicitTargetDate != null) {
            CustomJourney customJourney = matchingGroup.getCustomJourney();
            if (Boolean.TRUE.equals(customJourney.getIsLocked())) {
                throw new AppException(ErrorCode.JOURNEY_LOCKED);
            }
            Optional<GroupTrip> tripOpt = groupTripRepository.findByMatchingGroup(matchingGroup);
            if (tripOpt.isPresent() && tripOpt.get().getStatus() != GroupTripStatus.PLANNED) {
                throw new AppException(ErrorCode.JOURNEY_LOCKED);
            }
            if (customJourney.getStartDate() != null && customJourney.getEndDate() != null) {
                long duration = java.time.temporal.ChronoUnit.DAYS.between(customJourney.getStartDate(), customJourney.getEndDate());
                if (customJourney.getEndDate().isBefore(newTargetDate)) {
                    customJourney.setEndDate(newTargetDate.plusDays(Math.max(0, duration)));
                }
            }
            customJourney.setStartDate(newTargetDate);
        }

        matchingGroupRepository.save(matchingGroup);
        return getMatchingGroupById(groupId, userDetails);
    }

    @Transactional
    public MatchingGroupDetailResponse hideMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Hiding matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, userId);
        validateGroupNotInTerminalState(matchingGroup);

        if (matchingGroup.getStatus() == MatchingGroupStatus.HIDDEN) {
            return getMatchingGroupById(groupId, userDetails);
        }

        validatePlannedTripState(matchingGroup);

        matchingGroup.setStatus(MatchingGroupStatus.HIDDEN);
        matchingGroupRepository.save(matchingGroup);

        return getMatchingGroupById(groupId, userDetails);
    }

    @Transactional
    public MatchingGroupDetailResponse showMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Showing matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, userId);
        validateGroupNotInTerminalState(matchingGroup);

        if (matchingGroup.getStatus() != MatchingGroupStatus.HIDDEN) {
            return getMatchingGroupById(groupId, userDetails);
        }

        validatePlannedTripState(matchingGroup);
        validateTourAvailability(matchingGroup.getTour());

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (!matchingGroup.getMatchingDeadline().isAfter(now)
                || !matchingGroup.getTargetDate().isAfter(today)) {
            matchingGroup.setStatus(MatchingGroupStatus.CLOSED);
        } else if (matchingGroup.getCurrentSize() >= matchingGroup.getMaxSize()) {
            matchingGroup.setStatus(MatchingGroupStatus.FULL);
        } else {
            matchingGroup.setStatus(MatchingGroupStatus.OPEN);
        }

        matchingGroupRepository.save(matchingGroup);
        return getMatchingGroupById(groupId, userDetails);
    }

    @Transactional
    public MatchingGroupDetailResponse closeMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Closing matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, userId);
        validateGroupNotInTerminalState(matchingGroup);

        if (matchingGroup.getStatus() == MatchingGroupStatus.CLOSED) {
            return getMatchingGroupById(groupId, userDetails);
        }

        matchingGroup.setStatus(MatchingGroupStatus.CLOSED);
        matchingGroupRepository.save(matchingGroup);

        return getMatchingGroupById(groupId, userDetails);
    }

    @Transactional
    public MatchingGroupDetailResponse openMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Opening matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, userId);
        validateGroupNotInTerminalState(matchingGroup);

        if (matchingGroup.getStatus() == MatchingGroupStatus.OPEN) {
            return getMatchingGroupById(groupId, userDetails);
        }

        validateDatesInFuture(matchingGroup.getTargetDate(), matchingGroup.getMatchingDeadline());
        validateTourAvailability(matchingGroup.getTour());

        if (matchingGroup.getCurrentSize() >= matchingGroup.getMaxSize()) {
            matchingGroup.setStatus(MatchingGroupStatus.FULL);
        } else {
            matchingGroup.setStatus(MatchingGroupStatus.OPEN);
        }

        matchingGroupRepository.save(matchingGroup);
        return getMatchingGroupById(groupId, userDetails);
    }

    private void validateGroupLeader(MatchingGroup matchingGroup, UUID userId) {
        boolean isLeader = matchingMemberRepository.findByMatchingGroupAndUser(
                        matchingGroup,
                        userRepository.getReferenceById(userId)
                )
                .filter(m -> m.getRole() == MatchingRole.LEADER
                        && m.getStatus() == JoinStatus.ACCEPTED
                        && !Boolean.TRUE.equals(m.getIsDeleted()))
                .isPresent();

        if (!isLeader) {
            throw new AppException(ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE);
        }
    }

    private void validateGroupNotInTerminalState(MatchingGroup matchingGroup) {
        if (matchingGroup.getStatus() == MatchingGroupStatus.IN_PROGRESS
                || matchingGroup.getStatus() == MatchingGroupStatus.COMPLETED
                || matchingGroup.getStatus() == MatchingGroupStatus.CANCELLED) {
            throw new AppException(ErrorCode.MATCHING_GROUP_INVALID_STATE);
        }
    }

    private void validatePlannedTripState(MatchingGroup matchingGroup) {
        Optional<GroupTrip> tripOpt = groupTripRepository.findByMatchingGroup(matchingGroup);
        if (tripOpt.isPresent() && tripOpt.get().getStatus() != GroupTripStatus.PLANNED) {
            throw new AppException(ErrorCode.MATCHING_GROUP_INVALID_STATE);
        }
    }

    private void validateTourAvailability(Tour tour) {
        if (tour != null && (Boolean.TRUE.equals(tour.getIsDeleted())
                || tour.getStatus() != TourStatus.PUBLISHED
                || tour.getVendor() == null
                || tour.getVendor().getStatus() != VendorStatus.ACTIVE
                || Boolean.TRUE.equals(tour.getVendor().getIsDeleted()))) {
            throw new AppException(ErrorCode.MATCHING_TOUR_NOT_AVAILABLE);
        }
    }

    private void validateDatesInFuture(LocalDate targetDate, LocalDateTime matchingDeadline) {
        if (!targetDate.isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.MATCHING_TARGET_DATE_PASSED);
        }
        if (!matchingDeadline.isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.MATCHING_DEADLINE_PASSED);
        }
    }
}
