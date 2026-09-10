package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.GroupApplicationRequest;
import com.sep.treksphere.matching.dto.request.MatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.request.MyMatchingJoinRequestFilter;
import com.sep.treksphere.matching.dto.response.MatchingMemberResponse;
import com.sep.treksphere.matching.dto.response.MyMatchingJoinRequestResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.GroupJoinApplication;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.JoinApplicationStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.event.GroupApplicationDecidedEvent;
import com.sep.treksphere.matching.event.GroupApplicationSubmittedEvent;
import com.sep.treksphere.matching.event.GroupMembershipActivatedEvent;
import com.sep.treksphere.matching.mapper.MatchingGroupMapper;
import com.sep.treksphere.matching.repository.GroupJoinApplicationRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.MatchingGroupServiceImpl;
import com.sep.treksphere.notification.NotificationService;
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
import org.junit.jupiter.api.Disabled;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingGroupApplicationServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private GroupJoinApplicationRepository groupJoinApplicationRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @Spy
    private MatchingGroupMapper matchingGroupMapper = Mappers.getMapper(MatchingGroupMapper.class);

    @InjectMocks
    private MatchingGroupServiceImpl matchingGroupService;

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
        @DisplayName("Should successfully submit application to Tour-backed group")
        void submitApplication_success_tourGroup() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            GroupApplicationRequest request = new GroupApplicationRequest("Tôi có kinh nghiệm trekking 2 năm!");

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinStatus.ACCEPTED
            )).thenReturn(false);
            when(groupJoinApplicationRepository.existsByMatchingGroup_MatchingGroupIdAndApplicant_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinApplicationStatus.PENDING
            )).thenReturn(false);
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);

            when(groupJoinApplicationRepository.save(any(GroupJoinApplication.class))).thenAnswer(inv -> {
                GroupJoinApplication app = inv.getArgument(0);
                app.setApplicationId(UUID.randomUUID());
                return app;
            });

            MatchingMemberResponse response = matchingGroupService.submitApplication(groupId, request, applicantDetails);

            assertThat(response).isNotNull();
            assertThat(response.getRole()).isEqualTo(MatchingRole.MEMBER);
            assertThat(response.getStatus()).isEqualTo(JoinStatus.PENDING);
            assertThat(response.getUserId()).isEqualTo(applicantUser.getUserId());
            assertThat(response.getFullName()).isEqualTo("Applicant Tran");
            assertThat(response.getMessage()).isEqualTo("Tôi có kinh nghiệm trekking 2 năm!");
        }

        @Test
        @DisplayName("Should successfully submit application to Custom Journey group")
        void submitApplication_success_customJourneyGroup() {
            UUID groupId = openCustomJourneyGroup.getMatchingGroupId();
            GroupApplicationRequest request = new GroupApplicationRequest("Mong muốn tham gia săn mây cùng nhóm");

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openCustomJourneyGroup));
            when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinStatus.ACCEPTED
            )).thenReturn(false);
            when(groupJoinApplicationRepository.existsByMatchingGroup_MatchingGroupIdAndApplicant_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinApplicationStatus.PENDING
            )).thenReturn(false);
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);

            when(groupJoinApplicationRepository.save(any(GroupJoinApplication.class))).thenAnswer(inv -> {
                GroupJoinApplication app = inv.getArgument(0);
                app.setApplicationId(UUID.randomUUID());
                return app;
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
            when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinStatus.ACCEPTED
            )).thenReturn(false);
            when(groupJoinApplicationRepository.existsByMatchingGroup_MatchingGroupIdAndApplicant_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinApplicationStatus.PENDING
            )).thenReturn(false);
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

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinStatus.ACCEPTED
            )).thenReturn(true);

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.ALREADY_MEMBER));
        }

        @Test
        @DisplayName("Should throw 8109 JOIN_REQUEST_PENDING when applicant already has a pending application")
        void submitApplication_fail_alreadyPending() {
            UUID groupId = openTourGroup.getMatchingGroupId();

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinStatus.ACCEPTED
            )).thenReturn(false);
            when(groupJoinApplicationRepository.existsByMatchingGroup_MatchingGroupIdAndApplicant_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinApplicationStatus.PENDING
            )).thenReturn(true);

            assertThatThrownBy(() -> matchingGroupService.submitApplication(groupId, null, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.JOIN_REQUEST_PENDING));
        }

        @Test
        @DisplayName("Should successfully reapply and create new GroupJoinApplication when previously WITHDRAWN or REJECTED")
        void submitApplication_success_reapplyFromRejectedOrWithdrawn() {
            UUID groupId = openTourGroup.getMatchingGroupId();

            when(userRepository.findByIdForUpdate(applicantUser.getUserId())).thenReturn(Optional.of(applicantUser));
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinStatus.ACCEPTED
            )).thenReturn(false);
            when(groupJoinApplicationRepository.existsByMatchingGroup_MatchingGroupIdAndApplicant_UserIdAndStatusAndIsDeletedFalse(
                    groupId, applicantUser.getUserId(), JoinApplicationStatus.PENDING
            )).thenReturn(false);
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);

            UUID newAppId = UUID.randomUUID();
            when(groupJoinApplicationRepository.save(any(GroupJoinApplication.class))).thenAnswer(inv -> {
                GroupJoinApplication app = inv.getArgument(0);
                app.setApplicationId(newAppId);
                return app;
            });

            MatchingMemberResponse response = matchingGroupService.submitApplication(groupId, new GroupApplicationRequest("Nộp lại đơn"), applicantDetails);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(JoinStatus.PENDING);
            assertThat(response.getApplicationId()).isEqualTo(newAppId);
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

            GroupJoinApplication app1 = new GroupJoinApplication();
            app1.setApplicationId(UUID.randomUUID());
            app1.setMatchingGroup(openTourGroup);
            app1.setApplicant(applicantUser);
            app1.setStatus(JoinApplicationStatus.PENDING);
            app1.setIsDeleted(false);

            GroupJoinApplication app2 = new GroupJoinApplication();
            app2.setApplicationId(UUID.randomUUID());
            app2.setMatchingGroup(openCustomJourneyGroup);
            app2.setApplicant(applicantUser);
            app2.setStatus(JoinApplicationStatus.ACCEPTED);
            app2.setIsDeleted(false);

            Page<GroupJoinApplication> page = new PageImpl<>(List.of(app1, app2), PageRequest.of(0, 10), 2);

            when(groupJoinApplicationRepository.findMyApplications(
                    eq(applicantUser.getUserId()),
                    isNull(),
                    any()
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
        @DisplayName("Should query with JoinApplicationStatus filter when specified")
        void getMyApplications_withStatusFilter() {
            MyMatchingJoinRequestFilter filter = new MyMatchingJoinRequestFilter();
            filter.setStatus(JoinApplicationStatus.PENDING);
            filter.setPage(0);
            filter.setSize(10);

            GroupJoinApplication app1 = new GroupJoinApplication();
            app1.setApplicationId(UUID.randomUUID());
            app1.setMatchingGroup(openTourGroup);
            app1.setApplicant(applicantUser);
            app1.setStatus(JoinApplicationStatus.PENDING);
            app1.setIsDeleted(false);

            Page<GroupJoinApplication> page = new PageImpl<>(List.of(app1), PageRequest.of(0, 10), 1);

            when(groupJoinApplicationRepository.findMyApplications(
                    eq(applicantUser.getUserId()),
                    eq(JoinApplicationStatus.PENDING),
                    any()
            )).thenReturn(page);

            PaginationResponse<MyMatchingJoinRequestResponse> result =
                    matchingGroupService.getMyJoinRequests(filter, applicantDetails);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(JoinStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("P3-S2: Withdraw Tests")
    class WithdrawTests {

        @Test
        @DisplayName("Should successfully withdraw application from PENDING to WITHDRAWN and set withdrawnAt")
        void withdrawApplication_success_fromPending() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            GroupJoinApplication pendingApp = new GroupJoinApplication();
            pendingApp.setApplicationId(UUID.randomUUID());
            pendingApp.setMatchingGroup(openTourGroup);
            pendingApp.setApplicant(applicantUser);
            pendingApp.setStatus(JoinApplicationStatus.PENDING);
            pendingApp.setIsDeleted(false);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(groupJoinApplicationRepository.findByMatchingGroupAndApplicantAndStatusAndIsDeletedFalse(
                    openTourGroup, applicantUser, JoinApplicationStatus.PENDING
            )).thenReturn(Optional.of(pendingApp));
            when(groupJoinApplicationRepository.save(any(GroupJoinApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            MatchingMemberResponse response = matchingGroupService.withdrawApplication(groupId, applicantDetails);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(JoinStatus.WITHDRAWN);
            assertThat(pendingApp.getStatus()).isEqualTo(JoinApplicationStatus.WITHDRAWN);
            assertThat(pendingApp.getWithdrawnAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw 8101 MATCHING_GROUP_NOT_FOUND when withdrawing from non-existent group")
        void withdrawApplication_fail_groupNotFound() {
            UUID nonExistentGroupId = UUID.randomUUID();
            when(matchingGroupRepository.findByIdForUpdate(nonExistentGroupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchingGroupService.withdrawApplication(nonExistentGroupId, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_NOT_FOUND));
        }

        @Test
        @DisplayName("Should throw 8115 NO_PENDING_JOIN_REQUEST when no pending application record found")
        void withdrawApplication_fail_recordNotFound() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(groupJoinApplicationRepository.findByMatchingGroupAndApplicantAndStatusAndIsDeletedFalse(
                    openTourGroup, applicantUser, JoinApplicationStatus.PENDING
            )).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchingGroupService.withdrawApplication(groupId, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.NO_PENDING_JOIN_REQUEST));
        }
    }

    @Nested
    @DisplayName("P3-S3: Leader Review Tests (Approve & Reject)")
    class LeaderReviewTests {

        private MatchingMember leaderMember;
        private GroupJoinApplication pendingApp;
        private UUID pendingAppId;

        @BeforeEach
        void setUpReview() {
            leaderMember = new MatchingMember();
            leaderMember.setMatchingMemberId(UUID.randomUUID());
            leaderMember.setMatchingGroup(openTourGroup);
            leaderMember.setUser(leaderUser);
            leaderMember.setRole(MatchingRole.LEADER);
            leaderMember.setStatus(JoinStatus.ACCEPTED);
            leaderMember.setIsDeleted(false);

            pendingAppId = UUID.randomUUID();
            pendingApp = new GroupJoinApplication();
            pendingApp.setApplicationId(pendingAppId);
            pendingApp.setMatchingGroup(openTourGroup);
            pendingApp.setApplicant(applicantUser);
            pendingApp.setStatus(JoinApplicationStatus.PENDING);
            pendingApp.setIsDeleted(false);
        }

        private void mockLeaderCheck() {
            when(userRepository.getReferenceById(leaderUser.getUserId())).thenReturn(leaderUser);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, leaderUser))
                    .thenReturn(Optional.of(leaderMember));
        }

        @Test
        @DisplayName("Leader should fetch list of PENDING applications successfully")
        void getJoinRequests_success_defaultPending() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            MatchingJoinRequestFilter filter = new MatchingJoinRequestFilter();

            when(matchingGroupRepository.findWithOwnerById(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findByGroupIdAndStatus(eq(groupId), eq(JoinApplicationStatus.PENDING), any()))
                    .thenReturn(new PageImpl<>(List.of(pendingApp), PageRequest.of(0, 10), 1));

            PaginationResponse<MatchingMemberResponse> response =
                    matchingGroupService.getJoinRequests(groupId, filter, leaderDetails);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getStatus()).isEqualTo(JoinStatus.PENDING);
        }

        @Test
        @DisplayName("Leader should fetch list of REJECTED applications successfully (historical records preserved!)")
        void getJoinRequests_success_filterRejected() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            MatchingJoinRequestFilter filter = new MatchingJoinRequestFilter();
            filter.setStatus(JoinApplicationStatus.REJECTED);

            GroupJoinApplication rejectedApp = new GroupJoinApplication();
            rejectedApp.setApplicationId(UUID.randomUUID());
            rejectedApp.setMatchingGroup(openTourGroup);
            rejectedApp.setApplicant(applicantUser);
            rejectedApp.setStatus(JoinApplicationStatus.REJECTED);
            rejectedApp.setRejectReason("Không đủ điều kiện thể lực");

            when(matchingGroupRepository.findWithOwnerById(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findByGroupIdAndStatus(eq(groupId), eq(JoinApplicationStatus.REJECTED), any()))
                    .thenReturn(new PageImpl<>(List.of(rejectedApp), PageRequest.of(0, 10), 1));

            PaginationResponse<MatchingMemberResponse> response =
                    matchingGroupService.getJoinRequests(groupId, filter, leaderDetails);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getStatus()).isEqualTo(JoinStatus.REJECTED);
            assertThat(response.getContent().get(0).getRejectReason()).isEqualTo("Không đủ điều kiện thể lực");
        }

        @Test
        @DisplayName("Non-leader should be rejected with 8116 MATCHING_GROUP_UNAUTHORIZED_MANAGE when viewing join requests")
        void getJoinRequests_fail_unauthorized_outsider() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            MatchingJoinRequestFilter filter = new MatchingJoinRequestFilter();

            when(matchingGroupRepository.findWithOwnerById(groupId)).thenReturn(Optional.of(openTourGroup));
            when(userRepository.getReferenceById(applicantUser.getUserId())).thenReturn(applicantUser);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchingGroupService.getJoinRequests(groupId, filter, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE));
        }

        @Test
        @DisplayName("Leader approves PENDING member when group has remaining capacity; increments currentSize")
        void approveMember_success_partialCapacity() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            openTourGroup.setCurrentSize(1);
            openTourGroup.setMaxSize(5);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser)).thenReturn(Optional.empty());
            when(matchingMemberRepository.save(any(MatchingMember.class))).thenAnswer(inv -> {
                MatchingMember m = inv.getArgument(0);
                m.setMatchingMemberId(UUID.randomUUID());
                return m;
            });

            MatchingMemberResponse response = matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(JoinStatus.ACCEPTED);
            assertThat(response.getRole()).isEqualTo(MatchingRole.MEMBER);
            assertThat(openTourGroup.getCurrentSize()).isEqualTo(2);
            assertThat(openTourGroup.getStatus()).isEqualTo(MatchingGroupStatus.OPEN);
            assertThat(pendingApp.getStatus()).isEqualTo(JoinApplicationStatus.ACCEPTED);
            assertThat(pendingApp.getReviewedBy()).isEqualTo(leaderUser);
        }

        @Test
        @DisplayName("Leader approves PENDING member reaching maxSize; group automatically transitions to FULL")
        void approveMember_success_reachesCapacity_groupFull() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            openTourGroup.setCurrentSize(1);
            openTourGroup.setMaxSize(2);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser)).thenReturn(Optional.empty());
            when(matchingMemberRepository.save(any(MatchingMember.class))).thenAnswer(inv -> {
                MatchingMember m = inv.getArgument(0);
                m.setMatchingMemberId(UUID.randomUUID());
                return m;
            });

            MatchingMemberResponse response = matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(JoinStatus.ACCEPTED);
            assertThat(openTourGroup.getCurrentSize()).isEqualTo(2);
            assertThat(openTourGroup.getStatus()).isEqualTo(MatchingGroupStatus.FULL);
        }

        @Test
        @DisplayName("Non-leader cannot approve members; throws 8116 MATCHING_GROUP_UNAUTHORIZED_MANAGE")
        void approveMember_fail_unauthorized() {
            UUID groupId = openTourGroup.getMatchingGroupId();

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(userRepository.getReferenceById(applicantUser.getUserId())).thenReturn(applicantUser);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchingGroupService.approveMember(groupId, pendingAppId, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE));
        }

        @Test
        @DisplayName("Cannot approve member when group is not OPEN (e.g. CLOSED); throws 8105")
        void approveMember_fail_groupNotOpen() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            openTourGroup.setStatus(MatchingGroupStatus.CLOSED);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();

            assertThatThrownBy(() -> matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_NOT_OPEN));
        }

        @Test
        @DisplayName("Cannot approve member when group accepted count has reached maxSize; throws 8107")
        void approveMember_fail_groupFull() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            openTourGroup.setMaxSize(2);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(2L);

            assertThatThrownBy(() -> matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_FULL));
        }

        @Test
        @DisplayName("Cannot approve already ACCEPTED member; throws 8124 MEMBER_ALREADY_APPROVED")
        void approveMember_fail_alreadyApproved() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            pendingApp.setStatus(JoinApplicationStatus.ACCEPTED);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));

            assertThatThrownBy(() -> matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MEMBER_ALREADY_APPROVED));
        }

        @Test
        @DisplayName("Cannot approve member with invalid status (WITHDRAWN); throws 8125 INVALID_MEMBER_STATUS")
        void approveMember_fail_invalidStatus_withdrawn() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            pendingApp.setStatus(JoinApplicationStatus.WITHDRAWN);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));

            assertThatThrownBy(() -> matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_MEMBER_STATUS));
        }

        @Test
        @DisplayName("Cannot approve member belonging to different group; throws 8126 CROSS_GROUP_ACTION_NOT_ALLOWED")
        void approveMember_fail_crossGroup() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            MatchingGroup otherGroup = new MatchingGroup();
            otherGroup.setMatchingGroupId(UUID.randomUUID());
            pendingApp.setMatchingGroup(otherGroup);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));

            assertThatThrownBy(() -> matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.CROSS_GROUP_ACTION_NOT_ALLOWED));
        }

        @Test
        @DisplayName("Leader rejects PENDING member successfully; status transitions to REJECTED; currentSize unchanged")
        void rejectMember_success() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            openTourGroup.setCurrentSize(1);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));
            when(groupJoinApplicationRepository.save(any(GroupJoinApplication.class))).thenAnswer(inv -> inv.getArgument(0));

            MatchingMemberResponse response = matchingGroupService.rejectMember(groupId, pendingAppId, leaderDetails);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(JoinStatus.REJECTED);
            assertThat(pendingApp.getStatus()).isEqualTo(JoinApplicationStatus.REJECTED);
            assertThat(pendingApp.getReviewedBy()).isEqualTo(leaderUser);
            assertThat(openTourGroup.getCurrentSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("Cannot reject already REJECTED member; throws 8127 MEMBER_ALREADY_REJECTED")
        void rejectMember_fail_alreadyRejected() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            pendingApp.setStatus(JoinApplicationStatus.REJECTED);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));

            assertThatThrownBy(() -> matchingGroupService.rejectMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MEMBER_ALREADY_REJECTED));
        }

        @Test
        @DisplayName("Cannot reject member in invalid status (ACCEPTED); throws 8125 INVALID_MEMBER_STATUS")
        void rejectMember_fail_invalidStatus_accepted() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            pendingApp.setStatus(JoinApplicationStatus.ACCEPTED);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));

            assertThatThrownBy(() -> matchingGroupService.rejectMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_MEMBER_STATUS));
        }
    }

    @Nested
    @DisplayName("P3-S4: Race Conditions & Access Security Regression Tests")
    class RaceAndEventRegressionTests {

        private MatchingMember leaderMember;
        private GroupJoinApplication pendingApp;
        private UUID pendingAppId;

        @BeforeEach
        void setUpRegression() {
            leaderMember = new MatchingMember();
            leaderMember.setMatchingMemberId(UUID.randomUUID());
            leaderMember.setMatchingGroup(openTourGroup);
            leaderMember.setUser(leaderUser);
            leaderMember.setRole(MatchingRole.LEADER);
            leaderMember.setStatus(JoinStatus.ACCEPTED);
            leaderMember.setIsDeleted(false);

            pendingAppId = UUID.randomUUID();
            pendingApp = new GroupJoinApplication();
            pendingApp.setApplicationId(pendingAppId);
            pendingApp.setMatchingGroup(openTourGroup);
            pendingApp.setApplicant(applicantUser);
            pendingApp.setStatus(JoinApplicationStatus.PENDING);
            pendingApp.setIsDeleted(false);
        }

        private void mockLeaderCheck() {
            when(userRepository.getReferenceById(leaderUser.getUserId())).thenReturn(leaderUser);
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, leaderUser))
                    .thenReturn(Optional.of(leaderMember));
        }

        @Test
        @DisplayName("Race condition: Last-slot contention - Second concurrent approval must be rejected with 8107")
        void raceCondition_lastSlotContention_concurrentApprovals() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            openTourGroup.setCurrentSize(1);
            openTourGroup.setMaxSize(2); // Only 1 spot remaining

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));
            when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED))
                    .thenReturn(2L); // Concurrent thread already occupied slot

            assertThatThrownBy(() -> matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MATCHING_GROUP_FULL));
        }

        @Test
        @DisplayName("Race condition: Leader approve vs Trekker withdraw - If withdrawn first, approve fails with 8125")
        void raceCondition_approveVsWithdraw_applicantAlreadyWithdrawn() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            pendingApp.setStatus(JoinApplicationStatus.WITHDRAWN);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            mockLeaderCheck();
            when(groupJoinApplicationRepository.findDetailById(pendingAppId)).thenReturn(Optional.of(pendingApp));

            assertThatThrownBy(() -> matchingGroupService.approveMember(groupId, pendingAppId, leaderDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_MEMBER_STATUS));
        }

        @Test
        @DisplayName("Access security regression: Non-member cannot perform leave group")
        void accessSecurity_nonMemberCannotPerformActiveMemberActions() {
            UUID groupId = openTourGroup.getMatchingGroupId();

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchingGroupService.leaveMatchingGroup(groupId, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_A_MEMBER));
        }

        @Test
        @DisplayName("Access security regression: WITHDRAWN or LEFT member cannot perform accepted member action (leave group)")
        void accessSecurity_leftMemberCannotPerformActiveMemberActions() {
            UUID groupId = openTourGroup.getMatchingGroupId();
            MatchingMember leftMember = new MatchingMember();
            leftMember.setMatchingMemberId(UUID.randomUUID());
            leftMember.setMatchingGroup(openTourGroup);
            leftMember.setUser(applicantUser);
            leftMember.setRole(MatchingRole.MEMBER);
            leftMember.setStatus(JoinStatus.LEFT);

            when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(openTourGroup));
            when(matchingMemberRepository.findByMatchingGroupAndUser(openTourGroup, applicantUser))
                    .thenReturn(Optional.of(leftMember));

            assertThatThrownBy(() -> matchingGroupService.leaveMatchingGroup(groupId, applicantDetails))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER));
        }
    }
}
