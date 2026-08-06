package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.dto.request.MatchingJoinRequestFilter;
import com.sep.treksphere.dto.request.OwnedMatchingGroupFilterRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingGroupServiceImplTest {

    @Mock private MatchingGroupRepository matchingGroupRepository;
    @Mock private MatchingMemberRepository matchingMemberRepository;
    @Mock private TourRepository tourRepository;
    @Mock private UserRepository userRepository;
    @Mock private MatchingGroupMapper matchingGroupMapper;

    @InjectMocks
    private MatchingGroupServiceImpl service;

    private User owner;
    private Tour tour;
    private MatchingGroupCreateRequest request;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUserId(UUID.randomUUID());
        owner.setFullName("Group owner");
        userDetails = new CustomUserDetails(owner);

        tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setTourName("Fansipan");
        tour.setStatus(TourStatus.APPROVED);
        tour.setMaxCapacity(10);

        request = new MatchingGroupCreateRequest();
        request.setTourId(tour.getTourId());
        request.setGroupName("  Fansipan team  ");
        request.setDescription("  Trek together  ");
        request.setMaxSize(4);
        request.setTargetDate(LocalDate.now().plusDays(10));
        request.setMatchingDeadline(LocalDateTime.now().plusDays(5));

        lenient().when(userRepository.findByIdForUpdate(owner.getUserId())).thenReturn(Optional.of(owner));
    }

    @Test
    void getMatchingGroups_NormalizesKeywordAndQueriesOnlyAvailableGroupsOfPublicTours() {
        MatchingGroupFilterRequest filter = new MatchingGroupFilterRequest();
        filter.setTourId(tour.getTourId());
        filter.setTargetDate(LocalDate.now().plusDays(10));
        filter.setKeyword("  fanSIPan  ");
        when(matchingGroupRepository.findAvailableMatchingGroups(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getMatchingGroups(filter);

        verify(matchingGroupRepository).findAvailableMatchingGroups(
                eq(com.sep.treksphere.enums.matching.MatchingGroupStatus.OPEN),
                eq(TourStatus.APPROVED),
                eq(tour.getTourId()),
                eq(filter.getTargetDate()),
                eq("fansipan"),
                any(LocalDate.class),
                any(LocalDateTime.class),
                any(Pageable.class)
        );
    }

    @Test
    void getMatchingGroups_TreatsBlankKeywordAsNoKeywordFilter() {
        MatchingGroupFilterRequest filter = new MatchingGroupFilterRequest();
        filter.setKeyword("   ");
        when(matchingGroupRepository.findAvailableMatchingGroups(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getMatchingGroups(filter);

        verify(matchingGroupRepository).findAvailableMatchingGroups(
                eq(com.sep.treksphere.enums.matching.MatchingGroupStatus.OPEN),
                eq(TourStatus.APPROVED),
                isNull(),
                isNull(),
                eq(""),
                any(LocalDate.class),
                any(LocalDateTime.class),
                any(Pageable.class)
        );
    }

    @Test
    void getOwnedMatchingGroups_ReturnsAllOwnedGroupsWithoutDateRestriction() {
        OwnedMatchingGroupFilterRequest filter = new OwnedMatchingGroupFilterRequest();
        filter.setKeyword("  fanSIPan  ");

        MatchingGroup closedPastGroup = new MatchingGroup();
        closedPastGroup.setTargetDate(LocalDate.now().minusDays(10));
        closedPastGroup.setStatus(MatchingGroupStatus.CLOSED);
        closedPastGroup.setIsDeleted(true);
        MatchingGroupResponse mappedResponse = new MatchingGroupResponse();

        when(matchingGroupRepository.findOwnedOrJoinedGroups(
                eq(owner.getUserId()), eq(MatchingRole.MEMBER), eq(JoinStatus.ACCEPTED),
                isNull(), eq("fansipan"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(closedPastGroup)));
        when(matchingGroupMapper.toResponse(closedPastGroup)).thenReturn(mappedResponse);

        PaginationResponse<MatchingGroupResponse> result =
                service.getOwnedMatchingGroups(filter, userDetails);

        assertThat(result.getContent()).containsExactly(mappedResponse);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(matchingGroupRepository).findOwnedOrJoinedGroups(
                eq(owner.getUserId()), eq(MatchingRole.MEMBER), eq(JoinStatus.ACCEPTED),
                isNull(), eq("fansipan"), any(Pageable.class));
    }

    @Test
    void getOwnedMatchingGroups_AppliesStatusFilter() {
        OwnedMatchingGroupFilterRequest filter = new OwnedMatchingGroupFilterRequest();
        filter.setStatus(MatchingGroupStatus.FULL);

        when(matchingGroupRepository.findOwnedOrJoinedGroups(
                eq(owner.getUserId()), eq(MatchingRole.MEMBER), eq(JoinStatus.ACCEPTED),
                eq(MatchingGroupStatus.FULL), eq(""), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getOwnedMatchingGroups(filter, userDetails);

        verify(matchingGroupRepository).findOwnedOrJoinedGroups(
                eq(owner.getUserId()), eq(MatchingRole.MEMBER), eq(JoinStatus.ACCEPTED),
                eq(MatchingGroupStatus.FULL), eq(""), any(Pageable.class));
    }

    @Test
    void getOwnedMatchingGroups_LimitsPageSizeAndFallsBackFromUnsupportedSortField() {
        OwnedMatchingGroupFilterRequest filter = new OwnedMatchingGroupFilterRequest();
        filter.setPage(-1);
        filter.setSize(1_000);
        filter.setSortBy("owner.password");
        filter.setSortDir("asc");

        when(matchingGroupRepository.findOwnedOrJoinedGroups(
                any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getOwnedMatchingGroups(filter, userDetails);

        verify(matchingGroupRepository).findOwnedOrJoinedGroups(
                eq(owner.getUserId()),
                eq(MatchingRole.MEMBER),
                eq(JoinStatus.ACCEPTED),
                isNull(),
                eq(""),
                eq(PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "createdAt")))
        );
    }

    @Test
    void getMatchingGroupById_ReturnsAcceptedMembersAndPendingViewerContext() {
        User viewer = new User();
        viewer.setUserId(UUID.randomUUID());
        viewer.setFullName("Pending viewer");

        MatchingGroup group = new MatchingGroup();
        group.setMatchingGroupId(UUID.randomUUID());
        group.setTour(tour);
        group.setOwner(owner);
        group.setStatus(MatchingGroupStatus.OPEN);
        group.setCurrentSize(1);
        group.setMaxSize(4);
        group.setTargetDate(LocalDate.now().plusDays(10));
        group.setMatchingDeadline(LocalDateTime.now().plusDays(5));

        MatchingMember ownerMember = new MatchingMember();
        ownerMember.setMatchingGroup(group);
        ownerMember.setUser(owner);
        ownerMember.setRole(MatchingRole.OWNER);
        ownerMember.setStatus(JoinStatus.ACCEPTED);
        group.getMembers().add(ownerMember);

        MatchingMember pendingMember = new MatchingMember();
        pendingMember.setMatchingGroup(group);
        pendingMember.setUser(viewer);
        pendingMember.setRole(MatchingRole.MEMBER);
        pendingMember.setStatus(JoinStatus.PENDING);
        group.getMembers().add(pendingMember);

        MatchingGroupDetailResponse response = new MatchingGroupDetailResponse();
        MatchingMemberResponse ownerResponse = new MatchingMemberResponse();
        when(matchingGroupRepository.findPublicDetailById(
                eq(group.getMatchingGroupId()), anyCollection(), eq(TourStatus.APPROVED)))
                .thenReturn(Optional.of(group));
        when(matchingGroupMapper.toDetailResponse(group)).thenReturn(response);
        when(matchingGroupMapper.toMemberResponse(ownerMember)).thenReturn(ownerResponse);

        MatchingGroupDetailResponse result = service.getMatchingGroupById(
                group.getMatchingGroupId(), new CustomUserDetails(viewer));

        assertThat(result.getMembers()).containsExactly(ownerResponse);
        assertThat(result.getIsOwner()).isFalse();
        assertThat(result.getMyMembershipStatus()).isEqualTo(JoinStatus.PENDING);
        assertThat(result.getCanJoin()).isFalse();
        assertThat(result.getCanLeave()).isTrue();
    }

    @Test
    void getMatchingGroupById_ReturnsAnonymousViewerContext() {
        MatchingGroup group = new MatchingGroup();
        group.setMatchingGroupId(UUID.randomUUID());
        group.setTour(tour);
        group.setOwner(owner);
        group.setStatus(MatchingGroupStatus.OPEN);
        group.setCurrentSize(1);
        group.setMaxSize(4);
        group.setTargetDate(LocalDate.now().plusDays(10));
        group.setMatchingDeadline(LocalDateTime.now().plusDays(5));

        MatchingGroupDetailResponse response = new MatchingGroupDetailResponse();
        when(matchingGroupRepository.findPublicDetailById(
                eq(group.getMatchingGroupId()), anyCollection(), eq(TourStatus.APPROVED)))
                .thenReturn(Optional.of(group));
        when(matchingGroupMapper.toDetailResponse(group)).thenReturn(response);

        MatchingGroupDetailResponse result = service.getMatchingGroupById(group.getMatchingGroupId(), null);

        assertThat(result.getIsOwner()).isFalse();
        assertThat(result.getMyMembershipStatus()).isNull();
        assertThat(result.getCanJoin()).isFalse();
        assertThat(result.getCanLeave()).isFalse();
    }

    @Test
    void createMatchingGroup_CreatesGroupForApprovedTourAndIndependentTargetDate() {
        stubNoActiveGroup();
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository.save(any(MatchingGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MatchingGroupDetailResponse expected = new MatchingGroupDetailResponse();
        when(matchingGroupMapper.toDetailResponse(any(MatchingGroup.class))).thenReturn(expected);

        MatchingGroupDetailResponse result = service.createMatchingGroup(request, userDetails);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<MatchingGroup> captor = ArgumentCaptor.forClass(MatchingGroup.class);
        verify(matchingGroupRepository).save(captor.capture());
        MatchingGroup savedGroup = captor.getValue();
        assertThat(savedGroup.getGroupName()).isEqualTo("Fansipan team");
        assertThat(savedGroup.getDescription()).isEqualTo("Trek together");
        assertThat(savedGroup.getCurrentSize()).isEqualTo(1);
        assertThat(savedGroup.getStatus()).isEqualTo(com.sep.treksphere.enums.matching.MatchingGroupStatus.OPEN);
        assertThat(savedGroup.getMembers()).singleElement().satisfies(member -> {
            assertThat(member.getUser()).isSameAs(owner);
            assertThat(member.getRole()).isEqualTo(MatchingRole.OWNER);
            assertThat(member.getStatus()).isEqualTo(JoinStatus.ACCEPTED);
        });
    }

    @Test
    void createMatchingGroup_RejectsOwnerWithActiveGroupForSameTour() {
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        eq(owner),
                        eq(tour),
                        anyCollection(),
                        any(LocalDateTime.class),
                        any(LocalDate.class)))
                .thenReturn(true);

        assertError(ErrorCode.ALREADY_HAS_ACTIVE_GROUP);

        verify(matchingGroupRepository, never()).save(any());
    }

    @Test
    void createMatchingGroup_AllowsOwnerToCreateActiveGroupForDifferentTour() {
        Tour anotherTour = new Tour();
        anotherTour.setTourId(UUID.randomUUID());
        anotherTour.setTourName("Ta Xua");
        anotherTour.setStatus(TourStatus.APPROVED);
        anotherTour.setMaxCapacity(8);
        request.setTourId(anotherTour.getTourId());

        when(tourRepository.findByTourIdAndIsDeletedFalse(anotherTour.getTourId()))
                .thenReturn(Optional.of(anotherTour));
        when(matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        eq(owner),
                        eq(anotherTour),
                        anyCollection(),
                        any(LocalDateTime.class),
                        any(LocalDate.class)))
                .thenReturn(false);
        when(matchingGroupRepository.save(any(MatchingGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupMapper.toDetailResponse(any(MatchingGroup.class)))
                .thenReturn(new MatchingGroupDetailResponse());

        service.createMatchingGroup(request, userDetails);

        verify(matchingGroupRepository).save(any(MatchingGroup.class));
    }

    @Test
    void createMatchingGroup_RejectsNameThatIsTooShortAfterTrimming() {
        request.setGroupName("  a  ");

        assertError(ErrorCode.VALIDATION_ERROR);

        verify(matchingGroupRepository, never())
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        any(), any(), anyCollection(), any(), any());
    }

    @Test
    void createMatchingGroup_RejectsTourThatIsNotApproved() {
        tour.setStatus(TourStatus.HIDDEN);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        assertError(ErrorCode.MATCHING_TOUR_NOT_APPROVED);

    }

    @Test
    void createMatchingGroup_RejectsSizeAboveTourCapacity() {
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        request.setMaxSize(11);

        assertError(ErrorCode.MATCHING_GROUP_SIZE_EXCEEDS_TOUR_CAPACITY);
    }

    @Test
    void createMatchingGroup_AllowsSizeEqualToTourCapacity() {
        request.setMaxSize(tour.getMaxCapacity());
        stubNoActiveGroup();
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository.save(any(MatchingGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupMapper.toDetailResponse(any(MatchingGroup.class)))
                .thenReturn(new MatchingGroupDetailResponse());

        service.createMatchingGroup(request, userDetails);

        ArgumentCaptor<MatchingGroup> captor = ArgumentCaptor.forClass(MatchingGroup.class);
        verify(matchingGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getMaxSize()).isEqualTo(tour.getMaxCapacity());
    }

    @Test
    void joinMatchingGroup_CreatesPendingRequest() {
        User joiner = createActiveJoiner();
        MatchingGroup group = createJoinableGroup();
        MatchingMemberResponse expected = new MatchingMemberResponse();

        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByMatchingGroupAndUser(group, joiner))
                .thenReturn(Optional.empty());
        when(matchingMemberRepository.save(any(MatchingMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupMapper.toMemberResponse(any(MatchingMember.class))).thenReturn(expected);

        MatchingMemberResponse result = service.joinMatchingGroup(group.getMatchingGroupId(), new CustomUserDetails(joiner));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<MatchingMember> captor = ArgumentCaptor.forClass(MatchingMember.class);
        verify(matchingMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getMatchingGroup()).isSameAs(group);
        assertThat(captor.getValue().getUser()).isSameAs(joiner);
        assertThat(captor.getValue().getRole()).isEqualTo(MatchingRole.MEMBER);
        assertThat(captor.getValue().getStatus()).isEqualTo(JoinStatus.PENDING);
    }

    @Test
    void joinMatchingGroup_ReusesRejectedRequest() {
        User joiner = createActiveJoiner();
        MatchingGroup group = createJoinableGroup();
        MatchingMember rejectedMember = new MatchingMember();
        rejectedMember.setMatchingGroup(group);
        rejectedMember.setUser(joiner);
        rejectedMember.setRole(MatchingRole.MEMBER);
        rejectedMember.setStatus(JoinStatus.REJECTED);
        rejectedMember.setIsDeleted(true);

        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByMatchingGroupAndUser(group, joiner))
                .thenReturn(Optional.of(rejectedMember));
        when(matchingMemberRepository.save(rejectedMember)).thenReturn(rejectedMember);
        when(matchingGroupMapper.toMemberResponse(rejectedMember)).thenReturn(new MatchingMemberResponse());

        service.joinMatchingGroup(group.getMatchingGroupId(), new CustomUserDetails(joiner));

        assertThat(rejectedMember.getStatus()).isEqualTo(JoinStatus.PENDING);
        assertThat(rejectedMember.getIsDeleted()).isFalse();
        verify(matchingMemberRepository).save(rejectedMember);
    }

    @Test
    void joinMatchingGroup_RejectsOwnerJoiningOwnGroup() {
        stubCurrentUser(owner);
        MatchingGroup group = createJoinableGroup();
        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));

        assertJoinError(group, owner, ErrorCode.MATCHING_OWNER_CANNOT_JOIN);

        verify(matchingMemberRepository, never()).save(any());
    }

    @Test
    void joinMatchingGroup_RejectsGroupWhoseTourIsNotPublic() {
        User joiner = createActiveJoiner();
        MatchingGroup group = createJoinableGroup();
        group.getTour().setStatus(TourStatus.HIDDEN);
        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));

        assertJoinError(group, joiner, ErrorCode.MATCHING_TOUR_NOT_AVAILABLE);
    }

    @Test
    void joinMatchingGroup_RejectsExpiredDeadline() {
        User joiner = createActiveJoiner();
        MatchingGroup group = createJoinableGroup();
        group.setMatchingDeadline(LocalDateTime.now().minusMinutes(1));
        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));

        assertJoinError(group, joiner, ErrorCode.MATCHING_DEADLINE_PASSED);
    }

    @Test
    void joinMatchingGroup_RejectsTargetDateThatHasArrived() {
        User joiner = createActiveJoiner();
        MatchingGroup group = createJoinableGroup();
        group.setTargetDate(LocalDate.now());
        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));

        assertJoinError(group, joiner, ErrorCode.MATCHING_TARGET_DATE_PASSED);
    }

    @Test
    void joinMatchingGroup_RejectsInactiveUser() {
        User joiner = new User();
        joiner.setUserId(UUID.randomUUID());
        joiner.setStatus(UserStatus.LOCKED);
        stubCurrentUser(joiner);
        MatchingGroup group = createJoinableGroup();

        assertJoinError(group, joiner, ErrorCode.USER_NOT_ACTIVE);

        verify(matchingGroupRepository, never()).findDetailById(any());
    }

    @Test
    void joinMatchingGroup_RejectsExistingPendingRequest() {
        User joiner = createActiveJoiner();
        MatchingGroup group = createJoinableGroup();
        MatchingMember pendingMember = createExistingMember(group, joiner, JoinStatus.PENDING);
        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByMatchingGroupAndUser(group, joiner))
                .thenReturn(Optional.of(pendingMember));

        assertJoinError(group, joiner, ErrorCode.JOIN_REQUEST_PENDING);

        verify(matchingMemberRepository, never()).save(any());
    }

    @Test
    void joinMatchingGroup_RejectsAcceptedMember() {
        User joiner = createActiveJoiner();
        MatchingGroup group = createJoinableGroup();
        MatchingMember acceptedMember = createExistingMember(group, joiner, JoinStatus.ACCEPTED);
        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByMatchingGroupAndUser(group, joiner))
                .thenReturn(Optional.of(acceptedMember));

        assertJoinError(group, joiner, ErrorCode.ALREADY_MEMBER);

        verify(matchingMemberRepository, never()).save(any());
    }

    @Test
    void joinMatchingGroup_RejectsGroupAtCapacity() {
        User joiner = createActiveJoiner();
        MatchingGroup group = createJoinableGroup();
        group.setMaxSize(1);
        when(matchingGroupRepository.findDetailById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));

        assertJoinError(group, joiner, ErrorCode.MATCHING_GROUP_FULL);

        verify(matchingMemberRepository, never()).save(any());
    }

    @Test
    void getJoinRequests_DefaultsToPendingAndUsesZeroBasedPagination() {
        MatchingGroup group = createJoinableGroup();
        MatchingJoinRequestFilter filter = new MatchingJoinRequestFilter();
        MatchingMember pendingMember = createExistingMember(group, createActiveUser(), JoinStatus.PENDING);
        MatchingMemberResponse mappedResponse = new MatchingMemberResponse();
        when(matchingGroupRepository.findWithOwnerById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));
        when(matchingMemberRepository.findJoinRequests(
                group.getMatchingGroupId(),
                JoinStatus.PENDING,
                MatchingRole.MEMBER,
                PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(pendingMember)));
        when(matchingGroupMapper.toMemberResponse(pendingMember)).thenReturn(mappedResponse);

        PaginationResponse<MatchingMemberResponse> result = service.getJoinRequests(
                group.getMatchingGroupId(), filter, userDetails);

        assertThat(result.getContent()).containsExactly(mappedResponse);
        assertThat(result.getPageNumber()).isZero();
    }

    @Test
    void getJoinRequests_AllowsRejectedFilter() {
        MatchingGroup group = createJoinableGroup();
        MatchingJoinRequestFilter filter = new MatchingJoinRequestFilter();
        filter.setStatus(JoinStatus.REJECTED);
        filter.setPage(2);
        filter.setSize(20);
        when(matchingGroupRepository.findWithOwnerById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));
        when(matchingMemberRepository.findJoinRequests(
                group.getMatchingGroupId(),
                JoinStatus.REJECTED,
                MatchingRole.MEMBER,
                PageRequest.of(2, 20)))
                .thenReturn(Page.empty());

        service.getJoinRequests(group.getMatchingGroupId(), filter, userDetails);

        verify(matchingMemberRepository).findJoinRequests(
                group.getMatchingGroupId(),
                JoinStatus.REJECTED,
                MatchingRole.MEMBER,
                PageRequest.of(2, 20));
    }

    @Test
    void getJoinRequests_RejectsAcceptedAndLeftFilters() {
        MatchingJoinRequestFilter filter = new MatchingJoinRequestFilter();
        filter.setStatus(JoinStatus.ACCEPTED);

        assertThatThrownBy(() -> service.getJoinRequests(UUID.randomUUID(), filter, userDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JOIN_REQUEST_FILTER_STATUS);

        filter.setStatus(JoinStatus.LEFT);
        assertThatThrownBy(() -> service.getJoinRequests(UUID.randomUUID(), filter, userDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JOIN_REQUEST_FILTER_STATUS);
        verify(matchingGroupRepository, never()).findWithOwnerById(any());
    }

    @Test
    void getJoinRequests_RejectsInvalidPagination() {
        MatchingJoinRequestFilter filter = new MatchingJoinRequestFilter();
        filter.setPage(-1);

        assertThatThrownBy(() -> service.getJoinRequests(UUID.randomUUID(), filter, userDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JOIN_REQUEST_PAGINATION);

        filter.setPage(0);
        filter.setSize(51);
        assertThatThrownBy(() -> service.getJoinRequests(UUID.randomUUID(), filter, userDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JOIN_REQUEST_PAGINATION);
        verify(matchingGroupRepository, never()).findWithOwnerById(any());
    }

    @Test
    void getJoinRequests_RejectsNonOwner() {
        User requester = createActiveUser();
        MatchingGroup group = createJoinableGroup();
        MatchingJoinRequestFilter filter = new MatchingJoinRequestFilter();
        when(matchingGroupRepository.findWithOwnerById(group.getMatchingGroupId()))
                .thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.getJoinRequests(
                group.getMatchingGroupId(), filter, new CustomUserDetails(requester)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_VIEW_JOIN_REQUESTS);

        verify(matchingMemberRepository, never()).findJoinRequests(any(), any(), any(), any());
    }

    private User createActiveJoiner() {
        User joiner = createActiveUser();
        stubCurrentUser(joiner);
        return joiner;
    }

    private User createActiveUser() {
        User joiner = new User();
        joiner.setUserId(UUID.randomUUID());
        joiner.setFullName("Joiner");
        joiner.setStatus(UserStatus.ACTIVE);
        return joiner;
    }

    private void stubCurrentUser(User user) {
        when(userRepository.findByIdForUpdate(user.getUserId())).thenReturn(Optional.of(user));
    }

    private MatchingGroup createJoinableGroup() {
        MatchingGroup group = new MatchingGroup();
        group.setMatchingGroupId(UUID.randomUUID());
        group.setTour(tour);
        group.setOwner(owner);
        group.setStatus(MatchingGroupStatus.OPEN);
        group.setCurrentSize(1);
        group.setMaxSize(4);
        group.setTargetDate(LocalDate.now().plusDays(10));
        group.setMatchingDeadline(LocalDateTime.now().plusDays(5));

        MatchingMember ownerMember = new MatchingMember();
        ownerMember.setMatchingGroup(group);
        ownerMember.setUser(owner);
        ownerMember.setRole(MatchingRole.OWNER);
        ownerMember.setStatus(JoinStatus.ACCEPTED);
        group.getMembers().add(ownerMember);
        return group;
    }

    private MatchingMember createExistingMember(MatchingGroup group, User user, JoinStatus status) {
        MatchingMember member = new MatchingMember();
        member.setMatchingGroup(group);
        member.setUser(user);
        member.setRole(MatchingRole.MEMBER);
        member.setStatus(status);
        return member;
    }

    private void assertJoinError(MatchingGroup group, User joiner, ErrorCode errorCode) {
        assertThatThrownBy(() -> service.joinMatchingGroup(group.getMatchingGroupId(), new CustomUserDetails(joiner)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void stubNoActiveGroup() {
        when(matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        eq(owner),
                        eq(tour),
                        anyCollection(),
                        any(LocalDateTime.class),
                        any(LocalDate.class)))
                .thenReturn(false);
    }

    private void assertError(ErrorCode errorCode) {
        assertThatThrownBy(() -> service.createMatchingGroup(request, userDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
        verify(matchingGroupRepository, never()).save(any());
    }
}
