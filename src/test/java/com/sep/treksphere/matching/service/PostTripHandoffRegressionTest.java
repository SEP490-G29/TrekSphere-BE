package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.MomentCreateRequest;
import com.sep.treksphere.matching.dto.request.MomentVisibilityUpdateRequest;
import com.sep.treksphere.matching.dto.request.PeerReviewCreateRequest;
import com.sep.treksphere.matching.dto.response.MomentResponse;
import com.sep.treksphere.matching.dto.response.PeerReviewResponse;
import com.sep.treksphere.matching.entity.GroupPeerReview;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.entity.Moment;
import com.sep.treksphere.matching.entity.MomentMedia;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.MomentStatus;
import com.sep.treksphere.matching.enums.MomentVisibility;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import com.sep.treksphere.matching.mapper.MomentMapper;
import com.sep.treksphere.matching.mapper.PeerReviewMapper;
import com.sep.treksphere.matching.repository.GroupPeerReviewRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.repository.MomentMediaRepository;
import com.sep.treksphere.matching.repository.MomentRepository;
import com.sep.treksphere.matching.service.impl.GroupPeerReviewServiceImpl;
import com.sep.treksphere.matching.service.impl.MomentServiceImpl;
import com.sep.treksphere.matching.service.impl.TrustScoreServiceImpl;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("P8-S5: Post-Trip Lifecycle E2E Regression & Invariant Test Suite")
class PostTripHandoffRegressionTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private MomentMediaRepository momentMediaRepository;

    @Mock
    private GroupPeerReviewRepository peerReviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TrustScoreService trustScoreService;

    @Mock
    private NotificationService notificationService;

    @Spy
    private MomentMapper momentMapper = Mappers.getMapper(MomentMapper.class);

    @Spy
    private PeerReviewMapper peerReviewMapper = new PeerReviewMapper();

    @InjectMocks
    private MomentServiceImpl momentService;

    @InjectMocks
    private GroupPeerReviewServiceImpl peerReviewService;

    private User leader;
    private User member1;
    private User member2;
    private User outsider;
    private MatchingGroup completedGroup;
    private GroupTrip endedTrip;
    private GroupTrip inProgressTrip;
    private MatchingMember leaderMember;
    private MatchingMember member1Member;

    @BeforeEach
    void setUp() {
        leader = new User();
        leader.setUserId(UUID.randomUUID());
        leader.setFullName("Leader Trekker");
        leader.setEmail("leader@treksphere.com");
        leader.setStatus(UserStatus.ACTIVE);
        leader.setTrustScore((short) 100);
        leader.setRoles(new HashSet<>());

        member1 = new User();
        member1.setUserId(UUID.randomUUID());
        member1.setFullName("Member One");
        member1.setEmail("member1@treksphere.com");
        member1.setStatus(UserStatus.ACTIVE);
        member1.setTrustScore((short) 100);
        member1.setRoles(new HashSet<>());

        member2 = new User();
        member2.setUserId(UUID.randomUUID());
        member2.setFullName("Member Two");
        member2.setEmail("member2@treksphere.com");
        member2.setStatus(UserStatus.ACTIVE);
        member2.setTrustScore((short) 100);
        member2.setRoles(new HashSet<>());

        outsider = new User();
        outsider.setUserId(UUID.randomUUID());
        outsider.setFullName("Outsider User");
        outsider.setEmail("outsider@treksphere.com");
        outsider.setStatus(UserStatus.ACTIVE);
        outsider.setTrustScore((short) 100);
        outsider.setRoles(new HashSet<>());

        completedGroup = new MatchingGroup();
        completedGroup.setMatchingGroupId(UUID.randomUUID());
        completedGroup.setGroupName("Chinh phục Fansipan 3N2Đ");
        completedGroup.setOwner(leader);
        completedGroup.setStatus(MatchingGroupStatus.COMPLETED);
        completedGroup.setCurrentSize(3);
        completedGroup.setMaxSize(5);

        endedTrip = new GroupTrip();
        endedTrip.setGroupTripId(UUID.randomUUID());
        endedTrip.setMatchingGroup(completedGroup);
        endedTrip.setStatus(GroupTripStatus.ENDED);

        inProgressTrip = new GroupTrip();
        inProgressTrip.setGroupTripId(UUID.randomUUID());
        inProgressTrip.setMatchingGroup(completedGroup);
        inProgressTrip.setStatus(GroupTripStatus.IN_PROGRESS);

        leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setMatchingGroup(completedGroup);
        leaderMember.setUser(leader);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);

        member1Member = new MatchingMember();
        member1Member.setMatchingMemberId(UUID.randomUUID());
        member1Member.setMatchingGroup(completedGroup);
        member1Member.setUser(member1);
        member1Member.setRole(MatchingRole.MEMBER);
        member1Member.setStatus(JoinStatus.ACCEPTED);
    }

    @Nested
    @DisplayName("1. Post-Trip Moment & Profile Showcase Flow")
    class MomentLifecycleFlow {

        @Test
        @DisplayName("MEMBER_POST_TRIP_MOMENT_SUCCESS: Thành viên đăng khoảnh khắc sau chuyến đi và bật hiển thị Profile")
        void shouldAllowMemberToCreateAndShowcaseMomentAfterTrip() {
            when(userRepository.findById(member1.getUserId())).thenReturn(Optional.of(member1));
            when(matchingGroupRepository.findById(completedGroup.getMatchingGroupId()))
                    .thenReturn(Optional.of(completedGroup));
            when(matchingMemberRepository.findByGroupIdAndUserId(
                    completedGroup.getMatchingGroupId(), member1.getUserId()))
                    .thenReturn(Optional.of(member1Member));

            when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> {
                Moment m = invocation.getArgument(0);
                m.setMomentId(UUID.randomUUID());
                return m;
            });

            // When: Member creates moment
            MomentCreateRequest createReq = MomentCreateRequest.builder()
                    .caption("Đỉnh Fansipan lúc bình minh rực rỡ")
                    .placeName("Đỉnh Fansipan 3143m")
                    .visibility(MomentVisibility.GROUP_ONLY)
                    .mediaUrls(List.of("https://cloudinary.com/fansipan1.jpg"))
                    .build();

            MomentResponse created = momentService.createGroupMoment(completedGroup.getMatchingGroupId(), member1.getUserId(), createReq);

            // Then
            assertThat(created).isNotNull();
            assertThat(created.getCaption()).isEqualTo("Đỉnh Fansipan lúc bình minh rực rỡ");
            assertThat(created.getVisibility()).isEqualTo(MomentVisibility.GROUP_ONLY);

            // And When: Author toggles visibility to PUBLIC_PROFILE
            Moment groupMoment = Moment.builder()
                    .momentId(created.getMomentId())
                    .authorUser(member1)
                    .matchingGroup(completedGroup)
                    .authorMatchingMember(member1Member)
                    .caption("Đỉnh Fansipan lúc bình minh rực rỡ")
                    .visibility(MomentVisibility.GROUP_ONLY)
                    .status(MomentStatus.VISIBLE)
                    .mediaList(new ArrayList<>())
                    .build();

            when(momentRepository.findByMomentIdAndIsDeletedFalse(created.getMomentId())).thenReturn(Optional.of(groupMoment));
            when(momentRepository.save(any(Moment.class))).thenReturn(groupMoment);

            MomentVisibilityUpdateRequest visReq = MomentVisibilityUpdateRequest.builder()
                    .visibility(MomentVisibility.PUBLIC_PROFILE)
                    .build();

            MomentResponse updatedVis = momentService.updateGroupMomentVisibility(
                    completedGroup.getMatchingGroupId(), created.getMomentId(), member1.getUserId(), visReq);

            // Then: Moment is now showcased on public profile
            assertThat(updatedVis.getVisibility()).isEqualTo(MomentVisibility.PUBLIC_PROFILE);
        }

        @Test
        @DisplayName("OUTSIDER_FORBIDDEN_GROUP_MOMENT: Người ngoài không thể đăng khoảnh khắc nội bộ của nhóm")
        void shouldRejectOutsiderCreatingMomentInGroup() {
            when(userRepository.findById(outsider.getUserId())).thenReturn(Optional.of(outsider));
            when(matchingGroupRepository.findById(completedGroup.getMatchingGroupId()))
                    .thenReturn(Optional.of(completedGroup));
            when(matchingMemberRepository.findByGroupIdAndUserId(
                    completedGroup.getMatchingGroupId(), outsider.getUserId()))
                    .thenReturn(Optional.empty());

            MomentCreateRequest createReq = MomentCreateRequest.builder()
                    .caption("Spam nội bộ")
                    .mediaUrls(List.of("https://cloudinary.com/spam.jpg"))
                    .build();

            assertThatThrownBy(() -> momentService.createGroupMoment(completedGroup.getMatchingGroupId(), outsider.getUserId(), createReq))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
        }
    }

    @Nested
    @DisplayName("2. Post-Trip Peer Review & Trust Recalculation Flow")
    class PeerReviewAndTrustFlow {

        @Test
        @DisplayName("E2E_PEER_REVIEW_SUCCESS: Thành viên đánh giá chéo nhau sau khi chuyến đi kết thúc và kích hoạt tính điểm uy tín")
        void shouldSubmitPeerReviewsAndTriggerTrustRecalculation() {
            when(matchingGroupRepository.findById(completedGroup.getMatchingGroupId())).thenReturn(Optional.of(completedGroup));
            when(groupTripRepository.findByMatchingGroup(completedGroup)).thenReturn(Optional.of(endedTrip));
            when(matchingMemberRepository.findByGroupIdAndUserId(completedGroup.getMatchingGroupId(), leader.getUserId()))
                    .thenReturn(Optional.of(leaderMember));
            when(matchingMemberRepository.findByGroupIdAndUserId(completedGroup.getMatchingGroupId(), member1.getUserId()))
                    .thenReturn(Optional.of(member1Member));

            when(peerReviewRepository.existsByGroupTrip_GroupTripIdAndReviewerMatchingMember_MatchingMemberIdAndRevieweeMatchingMember_MatchingMemberIdAndIsDeletedFalse(
                    endedTrip.getGroupTripId(), leaderMember.getMatchingMemberId(), member1Member.getMatchingMemberId()))
                    .thenReturn(false);

            GroupPeerReview review = new GroupPeerReview();
            review.setGroupPeerReviewId(UUID.randomUUID());
            review.setGroupTrip(endedTrip);
            review.setReviewerMatchingMember(leaderMember);
            review.setRevieweeMatchingMember(member1Member);
            review.setActualEnduranceRating((short) 5);
            review.setPunctualityResponsibilityRating((short) 4);
            review.setFinancialFairnessRating((short) 5);
            review.setComment("Thành viên rất nhiệt tình, thể lực xuất sắc!");
            review.setModerationStatus(PeerReviewModerationStatus.VISIBLE);
            review.setCreatedAt(LocalDateTime.now());

            when(peerReviewRepository.save(any(GroupPeerReview.class))).thenReturn(review);

            // When: Leader reviews member1
            PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                    .revieweeUserId(member1.getUserId())
                    .actualEnduranceRating((short) 5)
                    .punctualityResponsibilityRating((short) 4)
                    .financialFairnessRating((short) 5)
                    .comment("Thành viên rất nhiệt tình, thể lực xuất sắc!")
                    .build();

            PeerReviewResponse response = peerReviewService.submitPeerReview(completedGroup.getMatchingGroupId(), leader.getUserId(), request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAverageRating()).isEqualTo(new BigDecimal("4.67"));
            assertThat(response.getModerationStatus()).isEqualTo(PeerReviewModerationStatus.VISIBLE);

            // Verify event-after-commit: TrustScoreService recalculation was triggered for reviewee
            verify(trustScoreService).recalculateTrustScore(member1.getUserId());
        }

        @Test
        @DisplayName("INVARIANT_BLOCK_PRE_COMPLETION_REVIEW: Chặn đánh giá chéo khi chuyến đi chưa kết thúc (IN_PROGRESS)")
        void shouldRejectPeerReviewWhenTripIsNotEnded() {
            when(matchingGroupRepository.findById(completedGroup.getMatchingGroupId())).thenReturn(Optional.of(completedGroup));
            when(groupTripRepository.findByMatchingGroup(completedGroup)).thenReturn(Optional.of(inProgressTrip));

            PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                    .revieweeUserId(member1.getUserId())
                    .actualEnduranceRating((short) 5)
                    .punctualityResponsibilityRating((short) 5)
                    .financialFairnessRating((short) 5)
                    .build();

            assertThatThrownBy(() -> peerReviewService.submitPeerReview(completedGroup.getMatchingGroupId(), leader.getUserId(), request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_NOT_ENDED_FOR_REVIEW);
        }

        @Test
        @DisplayName("INVARIANT_BLOCK_SELF_REVIEW: Chặn thành viên tự đánh giá chính mình")
        void shouldRejectSelfReview() {
            when(matchingGroupRepository.findById(completedGroup.getMatchingGroupId())).thenReturn(Optional.of(completedGroup));
            when(groupTripRepository.findByMatchingGroup(completedGroup)).thenReturn(Optional.of(endedTrip));
            when(matchingMemberRepository.findByGroupIdAndUserId(completedGroup.getMatchingGroupId(), leader.getUserId()))
                    .thenReturn(Optional.of(leaderMember));

            PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                    .revieweeUserId(leader.getUserId())
                    .actualEnduranceRating((short) 5)
                    .punctualityResponsibilityRating((short) 5)
                    .financialFairnessRating((short) 5)
                    .build();

            assertThatThrownBy(() -> peerReviewService.submitPeerReview(completedGroup.getMatchingGroupId(), leader.getUserId(), request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REVIEW_SELF);
        }

        @Test
        @DisplayName("INVARIANT_BLOCK_DUPLICATE_REVIEW: Chặn đánh giá trùng lặp trong cùng một chuyến đi")
        void shouldRejectDuplicatePeerReview() {
            when(matchingGroupRepository.findById(completedGroup.getMatchingGroupId())).thenReturn(Optional.of(completedGroup));
            when(groupTripRepository.findByMatchingGroup(completedGroup)).thenReturn(Optional.of(endedTrip));
            when(matchingMemberRepository.findByGroupIdAndUserId(completedGroup.getMatchingGroupId(), leader.getUserId()))
                    .thenReturn(Optional.of(leaderMember));
            when(matchingMemberRepository.findByGroupIdAndUserId(completedGroup.getMatchingGroupId(), member1.getUserId()))
                    .thenReturn(Optional.of(member1Member));

            // Already reviewed
            when(peerReviewRepository.existsByGroupTrip_GroupTripIdAndReviewerMatchingMember_MatchingMemberIdAndRevieweeMatchingMember_MatchingMemberIdAndIsDeletedFalse(
                    endedTrip.getGroupTripId(), leaderMember.getMatchingMemberId(), member1Member.getMatchingMemberId()))
                    .thenReturn(true);

            PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                    .revieweeUserId(member1.getUserId())
                    .actualEnduranceRating((short) 5)
                    .punctualityResponsibilityRating((short) 5)
                    .financialFairnessRating((short) 5)
                    .build();

            assertThatThrownBy(() -> peerReviewService.submitPeerReview(completedGroup.getMatchingGroupId(), leader.getUserId(), request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_REVIEWED_MEMBER);
        }
    }

    @Nested
    @DisplayName("3. Trust Score Projection Invariants & Formula Verification")
    class TrustScoreProjectionInvariants {

        @Test
        @DisplayName("FORMULA_PRECISION_TEST: Tính toán điểm uy tín theo trung bình cộng rating thực tế")
        void shouldCalculateTrustScoreAccurately() {
            TrustScoreServiceImpl realTrustService = new TrustScoreServiceImpl(userRepository, peerReviewRepository);

            GroupPeerReview r1 = new GroupPeerReview();
            r1.setActualEnduranceRating((short) 5);
            r1.setPunctualityResponsibilityRating((short) 5);
            r1.setFinancialFairnessRating((short) 5);
            r1.setModerationStatus(PeerReviewModerationStatus.VISIBLE);

            GroupPeerReview r2 = new GroupPeerReview();
            r2.setActualEnduranceRating((short) 4);
            r2.setPunctualityResponsibilityRating((short) 4);
            r2.setFinancialFairnessRating((short) 4);
            r2.setModerationStatus(PeerReviewModerationStatus.VISIBLE);

            when(userRepository.findById(member1.getUserId())).thenReturn(Optional.of(member1));
            when(peerReviewRepository.findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                    member1.getUserId(), PeerReviewModerationStatus.VISIBLE))
                    .thenReturn(List.of(r1, r2));

            // When
            realTrustService.recalculateTrustScore(member1.getUserId());

            // Then
            assertThat(member1.getTrustScore()).isEqualTo((short) 90);
            verify(userRepository).save(member1);
        }

        @Test
        @DisplayName("EXCLUDE_HIDDEN_OR_REPORTED_REVIEWS: Loại bỏ hoàn toàn đánh giá bị ẩn/report khỏi projection")
        void shouldExcludeHiddenOrReportedReviews() {
            TrustScoreServiceImpl realTrustService = new TrustScoreServiceImpl(userRepository, peerReviewRepository);

            when(userRepository.findById(member2.getUserId())).thenReturn(Optional.of(member2));
            when(peerReviewRepository.findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                    member2.getUserId(), PeerReviewModerationStatus.VISIBLE))
                    .thenReturn(Collections.emptyList());

            // When
            realTrustService.recalculateTrustScore(member2.getUserId());

            // Then: Default 100
            assertThat(member2.getTrustScore()).isEqualTo((short) 100);
            verify(userRepository).save(member2);
        }
    }
}
