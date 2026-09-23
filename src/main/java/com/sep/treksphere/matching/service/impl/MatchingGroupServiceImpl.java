package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.common.util.PaginationUtils;
import com.sep.treksphere.matching.dto.request.CustomJourneyCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupApplicationRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupUpdateRequest;
import com.sep.treksphere.matching.dto.request.MatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.request.MyMatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.request.MyMatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.request.RejectApplicationRequest;
import com.sep.treksphere.matching.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.matching.dto.response.MatchingGroupResponse;
import com.sep.treksphere.matching.dto.response.MatchingMemberResponse;
import com.sep.treksphere.matching.dto.response.MyMatchingJoinRequestResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import com.sep.treksphere.matching.entity.CustomJourneyCostItem;
import com.sep.treksphere.matching.entity.GroupExpense;
import com.sep.treksphere.matching.entity.GroupExpenseShare;
import com.sep.treksphere.matching.entity.GroupJoinApplication;
import com.sep.treksphere.matching.entity.GroupPost;
import com.sep.treksphere.matching.entity.GroupSettlement;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.entity.Moment;
import com.sep.treksphere.matching.enums.GroupContentStatus;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinApplicationStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.MomentStatus;
import com.sep.treksphere.matching.enums.MomentVisibility;
import com.sep.treksphere.matching.enums.SettlementStatus;
import com.sep.treksphere.matching.mapper.MatchingGroupMapper;
import com.sep.treksphere.matching.repository.GroupExpenseRepository;
import com.sep.treksphere.matching.repository.GroupExpenseShareRepository;
import com.sep.treksphere.matching.repository.GroupJoinApplicationRepository;
import com.sep.treksphere.matching.repository.GroupPostRepository;
import com.sep.treksphere.matching.repository.GroupSettlementRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.repository.MomentRepository;
import com.sep.treksphere.matching.service.GroupVoteService;
import com.sep.treksphere.matching.service.MatchingGroupService;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.tour.checkpoint.TourCheckpoint;
import com.sep.treksphere.tour.checkpoint.TourCheckpointRepository;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import com.sep.treksphere.vendor.VendorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingGroupServiceImpl implements MatchingGroupService {

    private static final Set<MatchingGroupStatus> ACTIVE_GROUP_STATUSES =
            Set.of(MatchingGroupStatus.OPEN, MatchingGroupStatus.FULL);

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final GroupJoinApplicationRepository groupJoinApplicationRepository;
    private final GroupTripRepository groupTripRepository;
    private final TourRepository tourRepository;
    private final TourCheckpointRepository tourCheckpointRepository;
    private final UserRepository userRepository;
    private final MatchingGroupMapper matchingGroupMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final GroupVoteService groupVoteService;
    private final GroupExpenseRepository groupExpenseRepository;
    private final GroupExpenseShareRepository groupExpenseShareRepository;
    private final GroupSettlementRepository groupSettlementRepository;
    private final GroupPostRepository groupPostRepository;
    private final MomentRepository momentRepository;

    @Override
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

        log.info("Fetching available matching groups with filters: sourceType={}, tourId={}, difficulty={}, location={}, minCost={}, maxCost={}, targetDate={}, targetDateFrom={}, targetDateTo={}, availableSlotsOnly={}, keyword={}",
                sourceType, filter.getTourId(), difficulty, location, filter.getMinCost(), filter.getMaxCost(), filter.getTargetDate(), filter.getTargetDateFrom(), filter.getTargetDateTo(), availableSlotsOnly, keyword);

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
                filter.getMinCost(),
                filter.getMaxCost(),
                availableSlotsOnly,
                keyword,
                today,
                now,
                filter.getPageable()
        );

        Page<MatchingGroupResponse> responsePage = groups.map(matchingGroupMapper::toResponse);
        applyCurrentLeaders(responsePage.getContent());
        return PaginationUtils.toPaginationResponse(responsePage);
    }

    @Override
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
                MatchingRole.LEADER,
                MatchingRole.MEMBER,
                JoinStatus.ACCEPTED,
                filter.getRole(),
                filter.getStatus(),
                filter.getTourId(),
                filter.getTargetDate(),
                keyword,
                filter.getPageable()
        );

        List<UUID> groupIds = groups.getContent().stream()
                .map(MatchingGroup::getMatchingGroupId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, MatchingMember> myMembershipsByGroupId = groupIds.isEmpty()
                ? Collections.emptyMap()
                : matchingMemberRepository.findByUserAndGroupIdsAndStatus(userId, groupIds, JoinStatus.ACCEPTED)
                        .stream()
                        .filter(m -> m.getMatchingGroup() != null && m.getMatchingGroup().getMatchingGroupId() != null)
                        .collect(Collectors.toMap(
                                m -> m.getMatchingGroup().getMatchingGroupId(),
                                m -> m,
                                (existing, duplicate) -> existing
                        ));

        Page<MatchingGroupResponse> responsePage = groups.map(group -> {
            MatchingGroupResponse response = matchingGroupMapper.toResponse(group);
            MatchingMember myMembership = myMembershipsByGroupId.get(group.getMatchingGroupId());
            if (myMembership == null && group.getMembers() != null) {
                myMembership = group.getMembers().stream()
                        .filter(member -> member.getUser() != null
                                && member.getUser().getUserId() != null
                                && member.getUser().getUserId().equals(userId)
                                && member.getStatus() == JoinStatus.ACCEPTED
                                && !Boolean.TRUE.equals(member.getIsDeleted()))
                        .findFirst()
                        .orElse(null);
            }
            boolean isLeader = myMembership != null && myMembership.getRole() == MatchingRole.LEADER;
            response.setIsOwner(isLeader);
            response.setMyRole(myMembership != null ? myMembership.getRole() : null);
            return response;
        });
        applyCurrentLeaders(responsePage.getContent());
        return PaginationUtils.toPaginationResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchingGroupDetailResponse getMatchingGroupById(UUID id, CustomUserDetails userDetails) {
        log.info("Fetching matching group detail: id={}", id);

        MatchingGroup matchingGroup = matchingGroupRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        UUID viewerId = userDetails == null ? null : userDetails.getUser().getUserId();
        MatchingMember viewerMembership = (viewerId == null || matchingGroup.getMembers() == null)
                ? null
                : matchingGroup.getMembers().stream()
                        .filter(member -> member.getUser().getUserId().equals(viewerId)
                                && !Boolean.TRUE.equals(member.getIsDeleted()))
                        .findFirst()
                        .orElse(null);

        JoinStatus membershipStatus = viewerMembership == null ? null : viewerMembership.getStatus();
        boolean isCurrentLeader = viewerMembership != null
                && viewerMembership.getRole() == MatchingRole.LEADER
                && membershipStatus == JoinStatus.ACCEPTED;
        boolean isAcceptedMember = membershipStatus == JoinStatus.ACCEPTED;
        boolean isOwner = viewerId != null
                && matchingGroup.getOwner() != null
                && matchingGroup.getOwner().getUserId().equals(viewerId);

        String myRejectReason = null;
        if (membershipStatus == null && viewerId != null && !isOwner) {
            List<GroupJoinApplication> userApps = groupJoinApplicationRepository
                    .findByMatchingGroup_MatchingGroupIdAndApplicant_UserIdOrderByCreatedAtDesc(id, viewerId);
            if (!userApps.isEmpty()) {
                GroupJoinApplication latestApp = userApps.get(0);
                if (latestApp.getStatus() == JoinApplicationStatus.PENDING) {
                    membershipStatus = JoinStatus.PENDING;
                } else if (latestApp.getStatus() == JoinApplicationStatus.REJECTED) {
                    membershipStatus = JoinStatus.REJECTED;
                    myRejectReason = latestApp.getRejectReason();
                }
            }
        }

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
        response.setMyRole(isAcceptedMember && viewerMembership != null ? viewerMembership.getRole() : null);
        response.setMyMembershipStatus(membershipStatus);
        response.setMyRejectReason(myRejectReason);
        response.setCanJoin(viewerId != null && !isOwner && !hasActiveMembership && groupIsJoinable);
        response.setCanLeave(viewerId != null && !isCurrentLeader && hasActiveMembership);
        
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
    public MatchingGroupDetailResponse createMatchingGroup(MatchingGroupCreateRequest request, UUID userId) {
        User currentUser = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        validateProfileCompleteness(currentUser);

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
        if (tour != null && customJourney == null) {
            customJourney = createCustomJourneyFromTour(tour, request, normalizedGroupName, normalizedDescription);
        }

        LocalDate startDate = request.getTargetDate();
        LocalDate endDate = customJourney != null && customJourney.getEndDate() != null
                ? customJourney.getEndDate()
                : startDate;
        validateNoScheduleConflict(currentUser.getUserId(), startDate, endDate, null, false);

        MatchingGroup matchingGroup = matchingGroupMapper.toEntity(request);
        matchingGroup.setTour(tour);
        matchingGroup.setOwner(currentUser);
        matchingGroup.setGroupName(normalizedGroupName);
        matchingGroup.setDescription(normalizedDescription);
        matchingGroup.setCoverImageUrl(normalizeNullableText(request.getCoverImageUrl()));

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

    /**
     * Điền `leaderName`/`leaderAvatarUrl` (Trưởng nhóm HIỆN TẠI, có thể khác owner sau khi bầu
     * Trưởng nhóm mới) cho danh sách response, gộp thành 1 query cho cả trang thay vì N+1.
     * Fallback về owner nếu vì lý do gì đó không tìm thấy accepted LEADER member (không nên xảy
     * ra bình thường vì mỗi nhóm luôn có đúng 1 leader).
     */
    private void applyCurrentLeaders(List<MatchingGroupResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        List<UUID> groupIds = responses.stream()
                .map(MatchingGroupResponse::getMatchingGroupId)
                .filter(Objects::nonNull)
                .toList();
        if (groupIds.isEmpty()) {
            return;
        }
        Map<UUID, MatchingMember> leadersByGroupId = matchingMemberRepository
                .findByGroupIdsAndRoleAndStatus(groupIds, MatchingRole.LEADER, JoinStatus.ACCEPTED)
                .stream()
                .filter(m -> m.getMatchingGroup() != null && m.getMatchingGroup().getMatchingGroupId() != null)
                .collect(Collectors.toMap(
                        m -> m.getMatchingGroup().getMatchingGroupId(),
                        m -> m,
                        (existing, duplicate) -> existing
                ));

        for (MatchingGroupResponse response : responses) {
            MatchingMember leader = leadersByGroupId.get(response.getMatchingGroupId());
            if (leader != null && leader.getUser() != null) {
                if (leader.getUser().getStatus() == com.sep.treksphere.user.UserStatus.LOCKED) {
                    response.setLeaderName(com.sep.treksphere.blog.BlogService.SYSTEM_USER_ANONYMOUS_NAME);
                    response.setLeaderAvatarUrl(null);
                } else {
                    response.setLeaderName(leader.getUser().getFullName());
                    response.setLeaderAvatarUrl(leader.getUser().getAvatarUrl());
                }
            } else {
                response.setLeaderName(response.getOwnerName());
                response.setLeaderAvatarUrl(response.getOwnerAvatarUrl());
            }
        }
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

        if (tour.getMaxCapacity() != null && request.getMaxSize() > tour.getMaxCapacity()) {
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

    private CustomJourney createCustomJourneyFromTour(
            Tour tour,
            MatchingGroupCreateRequest request,
            String normalizedGroupName,
            String normalizedDescription
    ) {
        CustomJourney journey = new CustomJourney();
        journey.setTitle(normalizedGroupName.isBlank() ? tour.getTourName() : normalizedGroupName);
        journey.setDescription(normalizedDescription != null ? normalizedDescription : tour.getDescription());
        if (tour.getDifficulty() != null) {
            try {
                journey.setDifficulty(JourneyDifficulty.valueOf(tour.getDifficulty().name()));
            } catch (IllegalArgumentException ex) {
                journey.setDifficulty(JourneyDifficulty.MODERATE);
            }
        } else {
            journey.setDifficulty(JourneyDifficulty.MODERATE);
        }

        LocalDate startDate = request.getTargetDate();
        int durationDays = (tour.getDurationDays() != null && tour.getDurationDays() > 0) ? tour.getDurationDays() : 1;
        LocalDate endDate = startDate.plusDays(durationDays - 1);
        journey.setStartDate(startDate);
        journey.setEndDate(endDate);
        journey.setIsLocked(false);

        List<TourCheckpoint> tourCheckpoints = tourCheckpointRepository
                .findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour);

        if (tourCheckpoints != null && !tourCheckpoints.isEmpty()) {
            Set<CustomJourneyCheckpoint> clonedCheckpoints = new HashSet<>();
            int order = 1;
            for (TourCheckpoint tcp : tourCheckpoints) {
                CustomJourneyCheckpoint cp = new CustomJourneyCheckpoint();
                cp.setCustomJourney(journey);
                cp.setCheckpointOrder(tcp.getCheckpointOrder() != null ? tcp.getCheckpointOrder() : order);
                cp.setTitle(tcp.getCheckpointName());
                cp.setDescription(tcp.getDescription());
                cp.setLocationName(tour.getLocation());
                cp.setLatitude(tcp.getLatitude());
                cp.setLongitude(tcp.getLongitude());
                cp.setImageUrl(tcp.getCheckpointImageUrl());
                cp.setDayNo(1);
                clonedCheckpoints.add(cp);
                order++;
            }
            journey.setCheckpoints(clonedCheckpoints);
        }

        return journey;
    }

    private static final List<MatchingGroupStatus> INACTIVE_GROUP_STATUSES = List.of(
            MatchingGroupStatus.CANCELLED,
            MatchingGroupStatus.COMPLETED
    );

    private LocalDate[] resolveGroupDateRange(MatchingGroup mg) {
        if (mg == null) {
            return null;
        }
        LocalDate start = mg.getTargetDate();
        LocalDate end = mg.getTargetDate();

        if (mg.getCustomJourney() != null) {
            if (mg.getCustomJourney().getStartDate() != null) {
                start = mg.getCustomJourney().getStartDate();
            }
            if (mg.getCustomJourney().getEndDate() != null) {
                end = mg.getCustomJourney().getEndDate();
            }
        } else if (mg.getTour() != null) {
            int durationDays = (mg.getTour().getDurationDays() != null && mg.getTour().getDurationDays() > 0)
                    ? mg.getTour().getDurationDays()
                    : 1;
            end = start.plusDays(durationDays - 1);
        }
        return new LocalDate[]{start, end};
    }

    private boolean isDateRangeOverlapping(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        if (startA == null || endA == null || startB == null || endB == null) {
            return false;
        }
        return !startA.isAfter(endB) && !endA.isBefore(startB);
    }

    private void validateNoScheduleConflict(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            UUID excludeGroupId,
            boolean isApplicant
    ) {
        if (userId == null || startDate == null || endDate == null) {
            return;
        }
        List<MatchingMember> activeMemberships = matchingMemberRepository
                .findActiveMembershipsWithSchedules(userId, JoinStatus.ACCEPTED, INACTIVE_GROUP_STATUSES);

        for (MatchingMember mm : activeMemberships) {
            MatchingGroup mg = mm.getMatchingGroup();
            if (mg == null || (excludeGroupId != null && excludeGroupId.equals(mg.getMatchingGroupId()))) {
                continue;
            }
            LocalDate[] range = resolveGroupDateRange(mg);
            if (range != null && isDateRangeOverlapping(startDate, endDate, range[0], range[1])) {
                log.warn("Schedule conflict detected for user {}: target [{}, {}] overlaps with group {} [{}, {}]",
                        userId, startDate, endDate, mg.getGroupName(), range[0], range[1]);
                if (isApplicant) {
                    throw new AppException(ErrorCode.APPLICANT_SCHEDULE_CONFLICT);
                }
                throw new AppException(ErrorCode.USER_SCHEDULE_CONFLICT);
            }
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
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

        validateProfileCompleteness(currentUser);

        UUID userId = currentUser.getUserId();
        log.info("Request to submit application to matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        boolean isCurrentLeader = matchingMemberRepository.findByMatchingGroupAndUser(
                        matchingGroup,
                        currentUser
                )
                .filter(m -> m.getRole() == MatchingRole.LEADER
                        && m.getStatus() == JoinStatus.ACCEPTED
                        && !Boolean.TRUE.equals(m.getIsDeleted()))
                .isPresent();

        if (isCurrentLeader) {
            throw new AppException(ErrorCode.MATCHING_OWNER_CANNOT_JOIN);
        }

        validateGroupOpenAndActive(matchingGroup);

        // Check if user has schedule conflict with another active group
        LocalDate[] targetRange = resolveGroupDateRange(matchingGroup);
        if (targetRange != null) {
            validateNoScheduleConflict(userId, targetRange[0], targetRange[1], groupId, false);
        }

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

        UUID currentLeaderUserId = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .filter(m -> m.getRole() == MatchingRole.LEADER)
                .map(m -> m.getUser().getUserId())
                .findFirst()
                .orElse(matchingGroup.getOwner().getUserId());

        notificationService.notify(
                currentLeaderUserId,
                NotificationEventType.GROUP_JOIN_REQUEST,
                ReferenceType.MATCHING_GROUP, matchingGroup.getMatchingGroupId(),
                "/trekker/my-groups/" + matchingGroup.getMatchingGroupId() + "?tab=members&subTab=requests",
                currentUser.getFullName(), matchingGroup.getGroupName());

        return matchingGroupMapper.toMemberResponse(savedApp);
    }

    @Override
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

    @Override
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

    @Override
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

        // Check if applicant has schedule conflict with another active group
        LocalDate[] targetRange = resolveGroupDateRange(matchingGroup);
        if (targetRange != null) {
            validateNoScheduleConflict(application.getApplicant().getUserId(), targetRange[0], targetRange[1], groupId, true);
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

        notificationService.notify(
                application.getApplicant().getUserId(),
                NotificationEventType.GROUP_MEMBER_APPROVED,
                ReferenceType.MATCHING_GROUP, matchingGroup.getMatchingGroupId(),
                "/trekker/my-groups/" + matchingGroup.getMatchingGroupId(),
                matchingGroup.getGroupName());

        MatchingMemberResponse response = matchingGroupMapper.toMemberResponse(savedMember);
        response.setApplicationId(application.getApplicationId());
        return response;
    }

    @Override
    @Transactional
    public MatchingMemberResponse rejectMember(
            UUID groupId,
            UUID applicationId,
            RejectApplicationRequest request,
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
        if (request != null && request.getRejectReason() != null && !request.getRejectReason().isBlank()) {
            application.setRejectReason(request.getRejectReason().trim());
        }

        GroupJoinApplication savedApp = groupJoinApplicationRepository.save(application);

        notificationService.notify(
                application.getApplicant().getUserId(),
                NotificationEventType.GROUP_MEMBER_REJECTED,
                ReferenceType.MATCHING_GROUP, matchingGroup.getMatchingGroupId(),
                "/trekker/my-join-requests",
                matchingGroup.getGroupName());

        return matchingGroupMapper.toMemberResponse(savedApp);
    }

    @Override
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

    @Override
    @Transactional
    public MatchingMemberResponse leaveMatchingGroup(UUID groupId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to leave matching group: groupId={}, userId={}",
                groupId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        if (matchingGroup.getStatus() == MatchingGroupStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.MATCHING_GROUP_INVALID_STATE);
        }

        Optional<GroupTrip> tripOpt = groupTripRepository.findByMatchingGroup(matchingGroup);
        if (tripOpt.isPresent() && tripOpt.get().getStatus() == GroupTripStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.MATCHING_GROUP_INVALID_STATE);
        }

        MatchingMember member = matchingMemberRepository.findByMatchingGroupAndUser(matchingGroup, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_A_MEMBER));

        if (Boolean.TRUE.equals(member.getIsDeleted()) || member.getStatus() != JoinStatus.ACCEPTED) {
            throw new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
        }

        if (member.getRole() == MatchingRole.LEADER
                || (matchingGroup.getOwner() != null && matchingGroup.getOwner().getUserId().equals(currentUser.getUserId()))) {
            throw new AppException(ErrorCode.OWNER_CANNOT_LEAVE);
        }

        validateMemberHasNoUnsettledExpenses(groupId, member.getMatchingMemberId());

        long acceptedCount = matchingMemberRepository
                .countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED);

        member.setStatus(JoinStatus.LEFT);
        member.setLeftAt(LocalDateTime.now());

        int newSize = Math.max(Math.toIntExact(acceptedCount) - 1, 1);
        matchingGroup.setCurrentSize(newSize);
        reevaluateGroupStatusAfterMemberLoss(matchingGroup, newSize);

        matchingGroupRepository.save(matchingGroup);

        MatchingMember savedMember = matchingMemberRepository.save(member);
        groupVoteService.handleMemberEligibilityLoss(savedMember);
        cleanupMemberGroupContent(groupId, member.getMatchingMemberId());

        List<UUID> recipientIds = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .map(m -> m.getUser().getUserId())
                .filter(id -> !id.equals(currentUser.getUserId()))
                .toList();

        notificationService.notify(
                recipientIds,
                NotificationEventType.GROUP_MEMBER_LEFT,
                ReferenceType.MATCHING_GROUP, matchingGroup.getMatchingGroupId(),
                "/trekker/my-groups/" + matchingGroup.getMatchingGroupId(),
                currentUser.getFullName(), matchingGroup.getGroupName());

        return matchingGroupMapper.toMemberResponse(savedMember);
    }

    @Override
    @Transactional
    public MatchingMemberResponse removeMember(UUID groupId, UUID memberId, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();
        log.info("Request to remove member from matching group: groupId={}, memberId={}, actorUserId={}",
                groupId, memberId, currentUser.getUserId());

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, currentUser.getUserId());

        MatchingMember target = matchingMemberRepository.findMemberByIdAndGroupId(memberId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_MEMBER_NOT_FOUND));

        if (Boolean.TRUE.equals(target.getIsDeleted()) || target.getStatus() != JoinStatus.ACCEPTED) {
            throw new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
        }

        if (target.getRole() == MatchingRole.LEADER) {
            throw new AppException(ErrorCode.MATCHING_MEMBER_CANNOT_REMOVE_LEADER);
        }

        validateMemberHasNoUnsettledExpenses(groupId, target.getMatchingMemberId());

        long acceptedCount = matchingMemberRepository
                .countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED);

        target.setStatus(JoinStatus.REMOVED);
        target.setLeftAt(LocalDateTime.now());

        int newSize = Math.max(Math.toIntExact(acceptedCount) - 1, 1);
        matchingGroup.setCurrentSize(newSize);
        reevaluateGroupStatusAfterMemberLoss(matchingGroup, newSize);

        matchingGroupRepository.save(matchingGroup);

        MatchingMember savedTarget = matchingMemberRepository.save(target);
        groupVoteService.handleMemberEligibilityLoss(savedTarget);
        cleanupMemberGroupContent(groupId, target.getMatchingMemberId());

        notificationService.notify(
                savedTarget.getUser().getUserId(),
                NotificationEventType.GROUP_MEMBER_REMOVED,
                ReferenceType.MATCHING_GROUP, matchingGroup.getMatchingGroupId(),
                "/trekker/my-groups",
                matchingGroup.getGroupName());

        return matchingGroupMapper.toMemberResponse(savedTarget);
    }

    private void validateMemberHasNoUnsettledExpenses(UUID groupId, UUID memberId) {
        List<GroupSettlement> settlements = groupSettlementRepository
                .findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId);
        boolean hasUnsettledSettlements = settlements.stream()
                .anyMatch(s -> s.getStatus() != SettlementStatus.CONFIRMED
                        && ((s.getFromMatchingMember() != null && memberId.equals(s.getFromMatchingMember().getMatchingMemberId()))
                        || (s.getToMatchingMember() != null && memberId.equals(s.getToMatchingMember().getMatchingMemberId()))));
        if (hasUnsettledSettlements) {
            log.warn("Member {} cannot leave/be removed from group {}: has pending settlements", memberId, groupId);
            throw new AppException(ErrorCode.MEMBER_HAS_UNSETTLED_EXPENSES);
        }

        List<GroupExpense> expenses = groupExpenseRepository
                .findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId);
        if (!expenses.isEmpty()) {
            BigDecimal totalPaid = BigDecimal.ZERO;
            BigDecimal totalShare = BigDecimal.ZERO;

            for (GroupExpense expense : expenses) {
                if (Boolean.TRUE.equals(expense.getIsDeleted())) {
                    continue;
                }
                if (expense.getPaidBy() != null && memberId.equals(expense.getPaidBy().getMatchingMemberId())) {
                    totalPaid = totalPaid.add(expense.getAmount() != null ? expense.getAmount() : BigDecimal.ZERO);
                }
                if (expense.getShares() != null) {
                    for (GroupExpenseShare share : expense.getShares()) {
                        if (!Boolean.TRUE.equals(share.getIsDeleted())
                                && share.getMatchingMember() != null
                                && memberId.equals(share.getMatchingMember().getMatchingMemberId())) {
                            totalShare = totalShare.add(share.getShareAmount() != null ? share.getShareAmount() : BigDecimal.ZERO);
                        }
                    }
                }
            }

            BigDecimal confirmedReceived = settlements.stream()
                    .filter(s -> s.getStatus() == SettlementStatus.CONFIRMED
                            && s.getToMatchingMember() != null
                            && memberId.equals(s.getToMatchingMember().getMatchingMemberId()))
                    .map(GroupSettlement::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal confirmedPaid = settlements.stream()
                    .filter(s -> s.getStatus() == SettlementStatus.CONFIRMED
                            && s.getFromMatchingMember() != null
                            && memberId.equals(s.getFromMatchingMember().getMatchingMemberId()))
                    .map(GroupSettlement::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netBalance = totalPaid.subtract(totalShare).add(confirmedReceived).subtract(confirmedPaid);
            if (netBalance.abs().compareTo(new BigDecimal("0.01")) > 0) {
                log.warn("Member {} cannot leave/be removed from group {}: unsettled net balance = {}", memberId, groupId, netBalance);
                throw new AppException(ErrorCode.MEMBER_HAS_UNSETTLED_EXPENSES);
            }
        }
    }

    private void cleanupMemberGroupContent(UUID groupId, UUID memberId) {
        List<GroupPost> posts = groupPostRepository
                .findByMatchingGroup_MatchingGroupIdAndPostedBy_MatchingMemberIdAndIsDeletedFalse(groupId, memberId);
        if (!posts.isEmpty()) {
            for (GroupPost post : posts) {
                post.setStatus(GroupContentStatus.HIDDEN);
            }
            groupPostRepository.saveAll(posts);
            log.info("Hidden {} posts for member {} in group {}", posts.size(), memberId, groupId);
        }

        List<Moment> moments = momentRepository
                .findByMatchingGroup_MatchingGroupIdAndAuthorMatchingMember_MatchingMemberIdAndIsDeletedFalse(groupId, memberId);
        if (!moments.isEmpty()) {
            for (Moment moment : moments) {
                if (moment.getVisibility() == MomentVisibility.PUBLIC_PROFILE) {
                    moment.setMatchingGroup(null);
                    moment.setAuthorMatchingMember(null);
                } else {
                    moment.setStatus(MomentStatus.HIDDEN);
                    moment.setMatchingGroup(null);
                    moment.setAuthorMatchingMember(null);
                }
            }
            momentRepository.saveAll(moments);
            log.info("Processed {} moments for member {} in group {}", moments.size(), memberId, groupId);
        }
    }


    private void reevaluateGroupStatusAfterMemberLoss(MatchingGroup matchingGroup, int newSize) {
        if (matchingGroup.getStatus() != MatchingGroupStatus.FULL) {
            return;
        }

        Tour tour = matchingGroup.getTour();
        boolean canReopen = newSize < matchingGroup.getMaxSize()
                && matchingGroup.getMatchingDeadline().isAfter(LocalDateTime.now())
                && matchingGroup.getTargetDate().isAfter(LocalDate.now())
                && (tour == null
                        || (!Boolean.TRUE.equals(tour.getIsDeleted())
                                && tour.getStatus() == TourStatus.PUBLISHED
                                && tour.getVendor() != null
                                && tour.getVendor().getStatus() == com.sep.treksphere.vendor.VendorStatus.ACTIVE
                                && !Boolean.TRUE.equals(tour.getVendor().getIsDeleted())));

        matchingGroup.setStatus(canReopen ? MatchingGroupStatus.OPEN : MatchingGroupStatus.CLOSED);
        log.info("Matching group {} status re-evaluated after member loss: groupId={}",
                canReopen ? "reopened (OPEN)" : "closed (CLOSED)", matchingGroup.getMatchingGroupId());
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

    @Override
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
        if (request.getCoverImageUrl() != null) {
            matchingGroup.setCoverImageUrl(normalizeNullableText(request.getCoverImageUrl()));
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
                if (tour.getMaxCapacity() != null && request.getMaxSize() > tour.getMaxCapacity()) {
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

    @Override
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

    @Override
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

        if (!matchingGroup.getMatchingDeadline().isAfter(now)) {
            matchingGroup.setStatus(MatchingGroupStatus.CLOSED);
        } else if (!matchingGroup.getTargetDate().isAfter(today)) {
            matchingGroup.setStatus(MatchingGroupStatus.CLOSED);
        } else if (matchingGroup.getCurrentSize() >= matchingGroup.getMaxSize()) {
            matchingGroup.setStatus(MatchingGroupStatus.FULL);
        } else {
            matchingGroup.setStatus(MatchingGroupStatus.OPEN);
        }

        matchingGroupRepository.save(matchingGroup);
        return getMatchingGroupById(groupId, userDetails);
    }

    @Override
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

    @Override
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

    @Override
    @Transactional
    public MatchingGroupDetailResponse startTrip(UUID groupId, CustomUserDetails userDetails) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Starting trip for matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, userId);

        if (matchingGroup.getStatus() == MatchingGroupStatus.IN_PROGRESS) {
            return getMatchingGroupById(groupId, userDetails);
        }

        if (matchingGroup.getStatus() == MatchingGroupStatus.COMPLETED
                || matchingGroup.getStatus() == MatchingGroupStatus.CANCELLED) {
            throw new AppException(ErrorCode.MATCHING_GROUP_INVALID_STATE);
        }

        LocalDate today = LocalDate.now();
        LocalDate tripEndDate = resolveTripEndDate(matchingGroup);
        if (tripEndDate != null && tripEndDate.isBefore(today)) {
            throw new AppException(ErrorCode.MATCHING_TARGET_DATE_PASSED);
        }

        matchingGroup.setStatus(MatchingGroupStatus.IN_PROGRESS);
        matchingGroupRepository.save(matchingGroup);

        groupTripRepository.findByMatchingGroup(matchingGroup).ifPresent(trip -> {
            trip.setStatus(GroupTripStatus.IN_PROGRESS);
            if (trip.getStartedAt() == null) {
                trip.setStartedAt(LocalDateTime.now());
            }
            groupTripRepository.save(trip);
        });

        List<UUID> memberIdsToNotifyStart = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .map(m -> m.getUser().getUserId())
                .toList();
        notificationService.notify(
                memberIdsToNotifyStart,
                NotificationEventType.GROUP_TRIP_STARTED,
                ReferenceType.GROUP_TRIP, groupId,
                "/trekker/my-groups/" + groupId + "?tab=itinerary",
                matchingGroup.getGroupName());

        return getMatchingGroupById(groupId, userDetails);
    }

    @Override
    @Transactional
    public MatchingGroupDetailResponse completeTrip(UUID groupId, CustomUserDetails userDetails) {
        UUID userId = userDetails.getUser().getUserId();
        log.info("Completing trip for matching group: groupId={}, userId={}", groupId, userId);

        MatchingGroup matchingGroup = matchingGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        validateGroupLeader(matchingGroup, userId);

        if (matchingGroup.getStatus() == MatchingGroupStatus.COMPLETED) {
            return getMatchingGroupById(groupId, userDetails);
        }

        if (matchingGroup.getStatus() != MatchingGroupStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.MATCHING_GROUP_INVALID_STATE);
        }

        matchingGroup.setStatus(MatchingGroupStatus.COMPLETED);
        matchingGroupRepository.save(matchingGroup);

        groupTripRepository.findByMatchingGroup(matchingGroup).ifPresent(trip -> {
            trip.setStatus(GroupTripStatus.ENDED);
            if (trip.getStartedAt() == null) {
                trip.setStartedAt(LocalDateTime.now());
            }
            if (trip.getEndedAt() == null) {
                trip.setEndedAt(LocalDateTime.now());
            }
            groupTripRepository.save(trip);
        });

        List<UUID> memberIdsToNotifyEnd = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .map(m -> m.getUser().getUserId())
                .toList();
        notificationService.notify(
                memberIdsToNotifyEnd,
                NotificationEventType.GROUP_TRIP_ENDED,
                ReferenceType.GROUP_TRIP, groupId,
                "/trekker/my-groups/" + groupId + "?tab=members&subTab=reviews",
                matchingGroup.getGroupName());

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

    private void validateProfileCompleteness(User user) {
        if (user.getFullName() == null || user.getFullName().trim().length() < 2
                || user.getPhone() == null || user.getPhone().trim().isEmpty()
                || user.getDateOfBirth() == null
                || user.getExperienceLevel() == null
                || user.getPreferredDifficulty() == null) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Vui lòng cập nhật đầy đủ thông tin cá nhân và hồ sơ leo núi trước khi tiếp tục.");
        }
    }

    private LocalDate resolveTripEndDate(MatchingGroup matchingGroup) {
        if (matchingGroup.getCustomJourney() != null && matchingGroup.getCustomJourney().getEndDate() != null) {
            return matchingGroup.getCustomJourney().getEndDate();
        }
        if (matchingGroup.getTour() != null && matchingGroup.getTour().getDurationDays() != null && matchingGroup.getTargetDate() != null) {
            return matchingGroup.getTargetDate().plusDays(matchingGroup.getTour().getDurationDays());
        }
        return matchingGroup.getTargetDate();
    }
}
