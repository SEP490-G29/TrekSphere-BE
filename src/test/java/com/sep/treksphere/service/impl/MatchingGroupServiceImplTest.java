package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.request.MatchingGroupFilterRequest;
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

        when(matchingGroupRepository.findOwnedGroups(
                eq(owner.getUserId()), isNull(), eq("fansipan"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(closedPastGroup)));
        when(matchingGroupMapper.toResponse(closedPastGroup)).thenReturn(mappedResponse);

        PaginationResponse<MatchingGroupResponse> result =
                service.getOwnedMatchingGroups(filter, userDetails);

        assertThat(result.getContent()).containsExactly(mappedResponse);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(matchingGroupRepository).findOwnedGroups(
                eq(owner.getUserId()), isNull(), eq("fansipan"), any(Pageable.class));
    }

    @Test
    void getOwnedMatchingGroups_AppliesStatusFilter() {
        OwnedMatchingGroupFilterRequest filter = new OwnedMatchingGroupFilterRequest();
        filter.setStatus(MatchingGroupStatus.FULL);

        when(matchingGroupRepository.findOwnedGroups(
                eq(owner.getUserId()), eq(MatchingGroupStatus.FULL), eq(""), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getOwnedMatchingGroups(filter, userDetails);

        verify(matchingGroupRepository).findOwnedGroups(
                eq(owner.getUserId()), eq(MatchingGroupStatus.FULL), eq(""), any(Pageable.class));
    }

    @Test
    void getOwnedMatchingGroups_LimitsPageSizeAndFallsBackFromUnsupportedSortField() {
        OwnedMatchingGroupFilterRequest filter = new OwnedMatchingGroupFilterRequest();
        filter.setPage(-1);
        filter.setSize(1_000);
        filter.setSortBy("owner.password");
        filter.setSortDir("asc");

        when(matchingGroupRepository.findOwnedGroups(any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getOwnedMatchingGroups(filter, userDetails);

        verify(matchingGroupRepository).findOwnedGroups(
                eq(owner.getUserId()),
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
    void createMatchingGroup_RejectsOwnerWithOpenOrFullFutureGroup() {
        when(matchingGroupRepository
                .existsByOwnerAndStatusInAndTargetDateGreaterThanEqualAndIsDeletedFalse(
                        eq(owner), anyCollection(), any(LocalDate.class)))
                .thenReturn(true);

        assertError(ErrorCode.ALREADY_HAS_ACTIVE_GROUP);

        verify(tourRepository, never()).findByTourIdAndIsDeletedFalse(any());
    }

    @Test
    void createMatchingGroup_RejectsNameThatIsTooShortAfterTrimming() {
        request.setGroupName("  a  ");

        assertError(ErrorCode.VALIDATION_ERROR);

        verify(matchingGroupRepository, never())
                .existsByOwnerAndStatusInAndTargetDateGreaterThanEqualAndIsDeletedFalse(any(), anyCollection(), any());
    }

    @Test
    void createMatchingGroup_RejectsTourThatIsNotApproved() {
        stubNoActiveGroup();
        tour.setStatus(TourStatus.HIDDEN);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        assertError(ErrorCode.MATCHING_TOUR_NOT_APPROVED);

    }

    @Test
    void createMatchingGroup_RejectsSizeAboveTourCapacity() {
        stubApprovedTour();
        request.setMaxSize(11);

        assertError(ErrorCode.MATCHING_GROUP_SIZE_EXCEEDS_TOUR_CAPACITY);
    }

    private void stubNoActiveGroup() {
        when(matchingGroupRepository
                .existsByOwnerAndStatusInAndTargetDateGreaterThanEqualAndIsDeletedFalse(
                        eq(owner), anyCollection(), any(LocalDate.class)))
                .thenReturn(false);
    }

    private void stubApprovedTour() {
        stubNoActiveGroup();
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
    }

    private void assertError(ErrorCode errorCode) {
        assertThatThrownBy(() -> service.createMatchingGroup(request, userDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
        verify(matchingGroupRepository, never()).save(any());
    }
}
