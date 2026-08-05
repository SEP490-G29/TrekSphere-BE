package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.entity.MatchingGroup;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.matching.JoinStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

        when(userRepository.findByIdForUpdate(owner.getUserId())).thenReturn(Optional.of(owner));
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
