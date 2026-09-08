package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.GroupApplicationRequest;
import com.sep.treksphere.matching.dto.request.MyMatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.response.MatchingMemberResponse;
import com.sep.treksphere.matching.dto.response.MyMatchingJoinRequestResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.event.GroupApplicationSubmittedEvent;
import com.sep.treksphere.matching.mapper.MatchingGroupMapper;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingGroupApplicationServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private MatchingGroupMapper matchingGroupMapper = Mappers.getMapper(MatchingGroupMapper.class);

    @InjectMocks
    private MatchingGroupService matchingGroupService;

    private User leaderUser;
    private User applicantUser;
    private CustomUserDetails applicantDetails;
    private CustomUserDetails leaderDetails;
    private MatchingGroup openTourGroup;
    private MatchingGroup openCustomJourneyGroup;
    private Tour sampleTour;
    private Vendor sampleVendor;
    private CustomJourney sampleJourney;

    @BeforeEach
    void setUp() {
        leaderUser = new User();
        leaderUser.setUserId(UUID.randomUUID());
        leaderUser.setFullName("Leader Nguyen");
        leaderUser.setStatus(UserStatus.ACTIVE);

        applicantUser = new User();
        applicantUser.setUserId(UUID.randomUUID());
        applicantUser.setFullName("Applicant Tran");
        applicantUser.setStatus(UserStatus.ACTIVE);

        applicantDetails = new CustomUserDetails(applicantUser);
        leaderDetails = new CustomUserDetails(leaderUser);

        sampleVendor = new Vendor();
        sampleVendor.setVendorId(UUID.randomUUID());
        sampleVendor.setStatus(VendorStatus.ACTIVE);
        sampleVendor.setIsDeleted(false);

        sampleTour = new Tour();
        sampleTour.setTourId(UUID.randomUUID());
        sampleTour.setTourName("Fansipan Summit Trek");
        sampleTour.setDifficulty(DifficultyLevel.HARD);
        sampleTour.setStatus(TourStatus.PUBLISHED);
        sampleTour.setVendor(sampleVendor);
        sampleTour.setIsDeleted(false);

        openTourGroup = new MatchingGroup();
        openTourGroup.setMatchingGroupId(UUID.randomUUID());
        openTourGroup.setGroupName("Fansipan Weekend Warriors");
        openTourGroup.setOwner(leaderUser);
        openTourGroup.setTour(sampleTour);
        openTourGroup.setStatus(MatchingGroupStatus.OPEN);
        openTourGroup.setCurrentSize(1);
        openTourGroup.setMaxSize(5);
        openTourGroup.setTargetDate(LocalDate.now().plusDays(10));
        openTourGroup.setMatchingDeadline(LocalDateTime.now().plusDays(5));
        openTourGroup.setIsDeleted(false);

        sampleJourney = new CustomJourney();
        sampleJourney.setCustomJourneyId(UUID.randomUUID());
        sampleJourney.setTitle("Ta Xua Cloud Hunting");
        sampleJourney.setDifficulty(JourneyDifficulty.MODERATE);
        sampleJourney.setStartDate(LocalDate.now().plusDays(15));
        sampleJourney.setEndDate(LocalDate.now().plusDays(17));
        sampleJourney.setIsLocked(false);

        openCustomJourneyGroup = new MatchingGroup();
        openCustomJourneyGroup.setMatchingGroupId(UUID.randomUUID());
        openCustomJourneyGroup.setGroupName("Ta Xua Cloud Hunters");
        openCustomJourneyGroup.setOwner(leaderUser);
        openCustomJourneyGroup.setCustomJourney(sampleJourney);
        openCustomJourneyGroup.setStatus(MatchingGroupStatus.OPEN);
        openCustomJourneyGroup.setCurrentSize(1);
        openCustomJourneyGroup.setMaxSize(4);
        openCustomJourneyGroup.setTargetDate(LocalDate.now().plusDays(15));
        openCustomJourneyGroup.setMatchingDeadline(LocalDateTime.now().plusDays(8));
        openCustomJourneyGroup.setIsDeleted(false);
    }

    @Nested
    @DisplayName("P3-S1: Submit Application Tests")
    class SubmitApplicationTests {

        @Test
        @DisplayName("Should successfully submit application to Tour-backed group and publish event")
        void submitApplication_success_tourGroup() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            GroupApplicationRequest request = new GroupApplicationRequest("Tôi có kinh nghiệm trekking 2 năm!");

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser)).thenReturn(Optional.empty());
            when(matchingMemberRepository.save(any(MatchingMember.class))).thenAnswer(inv -> {
                MatchingMember m = inv.getArgument(0);
                m.setMatchingMemberId(UUID.randomUUID());
                return m;
            });

            MatchingMemberResponse response = matchingGroupService.submitApplication(groupId, request, applicantDetails);

            assertThat(response).isNotNull();
            assertThat(response.getRole()).isEqualTo(MatchingRole.MEMBER);
            assertThat(response.getStatus()).isEqualTo(JoinStatus.PENDING);
            assertThat(response.getUserId()).isEqualTo(applicantUser.getUserId());
            assertThat(response.getFullName()).isEqualTo("Applicant Tran");
        }

        @Test
        @DisplayName("Should successfully submit application to Custom Journey group")
        void submitApplication_success_customJourneyGroup() {
            UUID groupId = openCustomJourneyGroup.getMatchingGroupId();
            GroupApplicationRequest request = new GroupApplicationRequest("Mong muốn tham gia săn mây cùng nhóm");

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openCustomJourneyGroup));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openCustomJourneyGroup, applicantUser)).thenReturn(Optional.empty());
            when(matchingMemberRepository.save(any(MatchingMember.class))).thenAnswer(inv -> {
                MatchingMember m = inv.getArgument(0);
                m.setMatchingMemberId(UUID.randomUUID());
                return m;
            });

            MatchingMemberResponse response = matchingGroupService.submitApplication(groupId, request, applicantDetails);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(JoinStatus.PENDING);
            assertThat(response.getRole()).isEqualTo(MatchingRole.MEMBER);
        }

        @Test
        @DisplayName("Should throw 8101 MATCHING_GROUP_NOT_FOUND when group does not exist")
        void submitApplication_fail_groupNotFound() {
            UUID nonExistentGroupId = UUID.randomUUID();
            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(nonExistentGroupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchingGroupService.submitApplication(nonExistentGroupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_NOT_FOUND));
        }

        @Test
        @DisplayName("Should throw 1002 USER_NOT_ACTIVE when applicant user account is inactive")
        void submitApplication_fail_userNotActive() {
            applicantUser.setStatus(UserStatus.LOCKED);
            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));

            assertThatThrownBy(() -> matchingGroupService.submitApplication(openTourGroup.getMatchingGroupId(), null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_ACTIVE));
        }

        @Test
        @DisplayName("Should throw 8123 MATCHING_OWNER_CANNOT_JOIN when leader applies to their own group")
        void submitApplication_fail_ownerCannotJoin() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            when(userRepository.findByIdForUpdate(leaderUser.getUserId())).thenReturn(Optional.of(leaderUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_OWNER_CANNOT_JOIN));
        }

        @Test
        @DisplayName("Should throw 8105 MATCHING_GROUP_NOT_OPEN when group is HIDDEN / CLOSED / CANCELLED")
        void submitApplication_fail_groupNotOpen() {
            openTourGroup.setStatus(MatchingGroupStatus.CLOSED);
            UUID groupId = openTourGroup.getMatchingGroupId();

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_NOT_OPEN));
        }

        @Test
        @DisplayName("Should throw 8107 MATCHING_GROUP_FULL when accepted member count equals maxSize")
        void submitApplication_fail_groupFull() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            openTourGroup.setMaxSize(3);

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(3L);

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_FULL));
        }

        @Test
        @DisplayName("Should throw 8106 MATCHING_DEADLINE_PASSED when matching deadline has expired")
        void submitApplication_fail_deadlinePassed() {
            openTourGroup.setMatchingDeadline(LocalDateTime.now().minusHours(1));
            UUID groupId = openTourGroup.getMatchingGroupId();

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_DEADLINE_PASSED));
        }

        @Test
        @DisplayName("Should throw 8124 MATCHING_TARGET_DATE_PASSED when target date has passed")
        void submitApplication_fail_targetDatePassed() {
            openTourGroup.setTargetDate(LocalDate.now().minusDays(1));
            UUID groupId = openTourGroup.getMatchingGroupId();

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_TARGET_DATE_PASSED));
        }

        @Test
        @DisplayName("Should throw 8121 MATCHING_TOUR_NOT_AVAILABLE when tour vendor is inactive or tour deleted")
        void submitApplication_fail_tourNotAvailable() {
            sampleVendor.setStatus(VendorStatus.SUSPENDED);
            UUID groupId = openTourGroup.getMatchingGroupId();

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_TOUR_NOT_AVAILABLE));
        }

        @Test
        @DisplayName("Should throw 8108 ALREADY_MEMBER when applicant is already an accepted member")
        void submitApplication_fail_alreadyMember() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            MatchingMember existingMember = new MatchingMember();
            existingMember.setMatchingGroup(openTourGroup);
            existingMember.setUser(applicantUser);
            existingMember.setRole(MatchingRole.MEMBER);
            existingMember.setStatus(JoinStatus.ACCEPTED);
            existingMember.setIsDeleted(false);

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser)).thenReturn(Optional.of(existingMember));

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ALREADY_MEMBER));
        }

        @Test
        @DisplayName("Should throw 8109 JOIN_REQUEST_PENDING when applicant already has a pending application")
        void submitApplication_fail_alreadyPending() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            MatchingMember existingPending = new MatchingMember();
            existingPending.setMatchingGroup(openTourGroup);
            existingPending.setUser(applicantUser);
            existingPending.setRole(MatchingRole.MEMBER);
            existingPending.setStatus(JoinStatus.PENDING);
            existingPending.setIsDeleted(false);

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser)).thenReturn(Optional.of(existingPending));

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.JOIN_REQUEST_PENDING));
        }

        @Test
        @DisplayName("Should successfully reapply on existing row when previously WITHDRAWN")
        void submitApplication_success_reapplyFromWithdrawn() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            MatchingMember withdrawnMember = new MatchingMember();
            withdrawnMember.setMatchingMemberId(UUID.randomUUID());
            withdrawnMember.setMatchingGroup(openTourGroup);
            withdrawnMember.setUser(applicantUser);
            withdrawnMember.setRole(MatchingRole.MEMBER);
            withdrawnMember.setStatus(JoinStatus.WITHDRAWN);
            withdrawnMember.setWithdrawnAt(LocalDateTime.now().minusDays(1));
            withdrawnMember.setIsDeleted(false);

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser)).thenReturn(Optional.of(withdrawnMember));
            when(matchingMemberRepository.save(any(MatchingMember.class))).thenAnswer(inv -> inv.getArgument(0));

            MatchingMemberResponse response = matchingGroupService.submitApplication(groupId, null, applicantDetails);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(JoinStatus.PENDING);
            assertThat(withdrawnMember.getWithdrawnAt()).isNull();
        }
    }

    @Nested
    @DisplayName("P3-S1: Get My Applications Tests")
    class GetMyApplicationsTests {

        @Test
        @DisplayName("Should return paginated list of user applications for both Tour and Custom Journey")
        void getMyApplications_success_tourAndCustomJourney() {
            MyMatchingJoinRequestFilter filter = new MyMatchingJoinRequestFilter();
            filter.setPage(0);
            filter.setSize(10);

            MatchingMember member1 = new MatchingMember();
            member1.setMatchingMemberId(UUID.randomUUID());
            member1.setMatchingGroup(openTourGroup);
            member1.setUser(applicantUser);
            member1.setRole(MatchingRole.MEMBER);
            member1.setStatus(JoinStatus.PENDING);
            member1.setIsDeleted(false);

            MatchingMember member2 = new MatchingMember();
            member2.setMatchingMemberId(UUID.randomUUID());
            member2.setMatchingGroup(openCustomJourneyGroup);
            member2.setUser(applicantUser);
            member2.setRole(MatchingRole.MEMBER);
            member2.setStatus(JoinStatus.ACCEPTED);
            member2.setIsDeleted(false);

            Page<MatchingMember> page = new PageImpl<>(List.of(member1, member2), PageRequest.of(0, 10), 2);

            when(matchingMemberRepository.findMyJoinRequests(
                    eq(applicantUser.getUserId()),
                    eq(MatchingRole.MEMBER),
                    isNull(),
                    any(PageRequest.class)
            )).thenReturn(page);

            PaginationResponse<MyMatchingJoinRequestResponse> result =
                    matchingGroupService.getMyJoinRequests(filter, applicantDetails);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);

            MyMatchingJoinRequestResponse resp1 = result.getContent().get(0);
            assertThat(resp1.getGroupName()).isEqualTo("Fansipan Weekend Warriors");
            assertThat(resp1.getSourceType()).isEqualTo(MatchingGroupSourceType.TOUR);
            assertThat(resp1.getTourName()).isEqualTo("Fansipan Summit Trek");
            assertThat(resp1.getDifficulty()).isEqualTo("HARD");
            assertThat(resp1.getStatus()).isEqualTo(JoinStatus.PENDING);
            assertThat(resp1.isCanCancel()).isTrue();
            assertThat(resp1.isCanWithdraw()).isTrue();

            MyMatchingJoinRequestResponse resp2 = result.getContent().get(1);
            assertThat(resp2.getGroupName()).isEqualTo("Ta Xua Cloud Hunters");
            assertThat(resp2.getSourceType()).isEqualTo(MatchingGroupSourceType.CUSTOM_JOURNEY);
            assertThat(resp2.getCustomJourneyTitle()).isEqualTo("Ta Xua Cloud Hunting");
            assertThat(resp2.getDifficulty()).isEqualTo("MODERATE");
            assertThat(resp2.getStatus()).isEqualTo(JoinStatus.ACCEPTED);
            assertThat(resp2.isCanCancel()).isFalse();
            assertThat(resp2.isCanWithdraw()).isFalse();
        }

        @Test
        @DisplayName("Should query with JoinStatus filter when specified")
        void getMyApplications_withStatusFilter() {
            MyMatchingJoinRequestFilter filter = new MyMatchingJoinRequestFilter();
            filter.setStatus(JoinStatus.PENDING);
            filter.setPage(0);
            filter.setSize(10);

            MatchingMember member1 = new MatchingMember();
            member1.setMatchingMemberId(UUID.randomUUID());
            member1.setMatchingGroup(openTourGroup);
            member1.setUser(applicantUser);
            member1.setRole(MatchingRole.MEMBER);
            member1.setStatus(JoinStatus.PENDING);
            member1.setIsDeleted(false);

            Page<MatchingMember> page = new PageImpl<>(List.of(member1), PageRequest.of(0, 10), 1);

            when(matchingMemberRepository.findMyJoinRequests(
                    eq(applicantUser.getUserId()),
                    eq(MatchingRole.MEMBER),
                    eq(JoinStatus.PENDING),
                    any(PageRequest.class)
            )).thenReturn(page);

            PaginationResponse<MyMatchingJoinRequestResponse> result =
                    matchingGroupService.getMyJoinRequests(filter, applicantDetails);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(JoinStatus.PENDING);
        }
    }
}
