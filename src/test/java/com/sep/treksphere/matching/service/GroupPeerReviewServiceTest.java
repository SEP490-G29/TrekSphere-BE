package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.PeerReviewCreateRequest;
import com.sep.treksphere.matching.dto.response.PeerReviewCandidateResponse;
import com.sep.treksphere.matching.dto.response.PeerReviewResponse;
import com.sep.treksphere.matching.entity.GroupPeerReview;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import com.sep.treksphere.matching.mapper.PeerReviewMapper;
import com.sep.treksphere.matching.repository.GroupPeerReviewRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.GroupPeerReviewServiceImpl;
import com.sep.treksphere.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupPeerReviewServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private GroupPeerReviewRepository groupPeerReviewRepository;

    @Mock
    private TrustScoreService trustScoreService;

    @Mock
    private com.sep.treksphere.user.UserRepository userRepository;

    @Spy
    private PeerReviewMapper peerReviewMapper = new PeerReviewMapper();

    @InjectMocks
    private GroupPeerReviewServiceImpl peerReviewService;

    private UUID groupId;
    private UUID tripId;
    private UUID reviewerUserId;
    private UUID revieweeUserId;
    private UUID reviewerMemberId;
    private UUID revieweeMemberId;

    private MatchingGroup group;
    private GroupTrip endedTrip;
    private GroupTrip inProgressTrip;
    private MatchingMember reviewerMember;
    private MatchingMember revieweeMember;
    private User reviewerUser;
    private User revieweeUser;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        reviewerUserId = UUID.randomUUID();
        revieweeUserId = UUID.randomUUID();
        reviewerMemberId = UUID.randomUUID();
        revieweeMemberId = UUID.randomUUID();

        reviewerUser = new User();
        reviewerUser.setUserId(reviewerUserId);
        reviewerUser.setFullName("Reviewer User");
        reviewerUser.setAvatarUrl("https://img.com/rev.jpg");

        revieweeUser = new User();
        revieweeUser.setUserId(revieweeUserId);
        revieweeUser.setFullName("Reviewee User");
        revieweeUser.setAvatarUrl("https://img.com/ee.jpg");

        group = new MatchingGroup();
        group.setMatchingGroupId(groupId);

        endedTrip = new GroupTrip();
        endedTrip.setGroupTripId(tripId);
        endedTrip.setMatchingGroup(group);
        endedTrip.setStatus(GroupTripStatus.ENDED);

        inProgressTrip = new GroupTrip();
        inProgressTrip.setGroupTripId(tripId);
        inProgressTrip.setMatchingGroup(group);
        inProgressTrip.setStatus(GroupTripStatus.IN_PROGRESS);

        reviewerMember = new MatchingMember();
        reviewerMember.setMatchingMemberId(reviewerMemberId);
        reviewerMember.setMatchingGroup(group);
        reviewerMember.setUser(reviewerUser);
        reviewerMember.setStatus(JoinStatus.ACCEPTED);
        reviewerMember.setRole(MatchingRole.MEMBER);

        revieweeMember = new MatchingMember();
        revieweeMember.setMatchingMemberId(revieweeMemberId);
        revieweeMember.setMatchingGroup(group);
        revieweeMember.setUser(revieweeUser);
        revieweeMember.setStatus(JoinStatus.ACCEPTED);
        revieweeMember.setRole(MatchingRole.LEADER);
    }

    @Test
    @DisplayName("Gửi đánh giá thành công khi chuyến đi đã ENDED và gọi recalculate Trust Score")
    void submitPeerReview_Success() {
        PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                .revieweeMemberId(revieweeMemberId)
                .actualEnduranceRating((short) 5)
                .punctualityResponsibilityRating((short) 4)
                .financialFairnessRating((short) 5)
                .comment("Đồng hành rất nhiệt tình và đúng giờ!")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(endedTrip));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, reviewerUserId))
                .thenReturn(Optional.of(reviewerMember));
        when(matchingMemberRepository.findMemberByIdAndGroupId(revieweeMemberId, groupId))
                .thenReturn(Optional.of(revieweeMember));
        when(groupPeerReviewRepository.existsByGroupTrip_GroupTripIdAndReviewerMatchingMember_MatchingMemberIdAndRevieweeMatchingMember_MatchingMemberIdAndIsDeletedFalse(
                tripId, reviewerMemberId, revieweeMemberId)).thenReturn(false);

        when(groupPeerReviewRepository.save(any(GroupPeerReview.class))).thenAnswer(invocation -> {
            GroupPeerReview saved = invocation.getArgument(0);
            saved.setGroupPeerReviewId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        PeerReviewResponse response = peerReviewService.submitPeerReview(groupId, reviewerUserId, request);

        assertNotNull(response);
        assertEquals(revieweeUserId, response.getRevieweeUserId());
        assertEquals((short) 5, response.getActualEnduranceRating());
        assertEquals((short) 4, response.getPunctualityResponsibilityRating());
        assertEquals((short) 5, response.getFinancialFairnessRating());
        assertEquals(PeerReviewModerationStatus.VISIBLE, response.getModerationStatus());

        verify(trustScoreService, times(1)).recalculateTrustScore(revieweeUserId);
    }

    @Test
    @DisplayName("Chặn đánh giá khi chuyến đi chưa kết thúc (TRIP_NOT_ENDED_FOR_REVIEW)")
    void submitPeerReview_ThrowsWhenTripNotCompleted() {
        PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                .revieweeMemberId(revieweeMemberId)
                .actualEnduranceRating((short) 5)
                .punctualityResponsibilityRating((short) 5)
                .financialFairnessRating((short) 5)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(inProgressTrip));

        AppException ex = assertThrows(AppException.class, () ->
                peerReviewService.submitPeerReview(groupId, reviewerUserId, request));

        assertEquals(ErrorCode.TRIP_NOT_ENDED_FOR_REVIEW, ex.getErrorCode());
        verify(groupPeerReviewRepository, never()).save(any());
        verify(trustScoreService, never()).recalculateTrustScore(any());
    }

    @Test
    @DisplayName("Chặn tự đánh giá chính mình (CANNOT_REVIEW_SELF)")
    void submitPeerReview_ThrowsWhenSelfReview() {
        PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                .revieweeMemberId(reviewerMemberId)
                .actualEnduranceRating((short) 5)
                .punctualityResponsibilityRating((short) 5)
                .financialFairnessRating((short) 5)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(endedTrip));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, reviewerUserId))
                .thenReturn(Optional.of(reviewerMember));
        when(matchingMemberRepository.findMemberByIdAndGroupId(reviewerMemberId, groupId))
                .thenReturn(Optional.of(reviewerMember));

        AppException ex = assertThrows(AppException.class, () ->
                peerReviewService.submitPeerReview(groupId, reviewerUserId, request));

        assertEquals(ErrorCode.CANNOT_REVIEW_SELF, ex.getErrorCode());
        verify(groupPeerReviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chặn đánh giá trùng lặp cùng một thành viên trong chuyến đi (ALREADY_REVIEWED_MEMBER)")
    void submitPeerReview_ThrowsWhenDuplicateReview() {
        PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                .revieweeMemberId(revieweeMemberId)
                .actualEnduranceRating((short) 4)
                .punctualityResponsibilityRating((short) 4)
                .financialFairnessRating((short) 4)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(endedTrip));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, reviewerUserId))
                .thenReturn(Optional.of(reviewerMember));
        when(matchingMemberRepository.findMemberByIdAndGroupId(revieweeMemberId, groupId))
                .thenReturn(Optional.of(revieweeMember));
        when(groupPeerReviewRepository.existsByGroupTrip_GroupTripIdAndReviewerMatchingMember_MatchingMemberIdAndRevieweeMatchingMember_MatchingMemberIdAndIsDeletedFalse(
                tripId, reviewerMemberId, revieweeMemberId)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                peerReviewService.submitPeerReview(groupId, reviewerUserId, request));

        assertEquals(ErrorCode.ALREADY_REVIEWED_MEMBER, ex.getErrorCode());
        verify(groupPeerReviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chặn đánh giá với điểm rating không hợp lệ (INVALID_RATING_VALUE)")
    void submitPeerReview_ThrowsWhenRatingInvalid() {
        PeerReviewCreateRequest request = PeerReviewCreateRequest.builder()
                .revieweeMemberId(revieweeMemberId)
                .actualEnduranceRating((short) 6) // Vượt quá 5
                .punctualityResponsibilityRating((short) 5)
                .financialFairnessRating((short) 5)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(endedTrip));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, reviewerUserId))
                .thenReturn(Optional.of(reviewerMember));
        when(matchingMemberRepository.findMemberByIdAndGroupId(revieweeMemberId, groupId))
                .thenReturn(Optional.of(revieweeMember));

        AppException ex = assertThrows(AppException.class, () ->
                peerReviewService.submitPeerReview(groupId, reviewerUserId, request));

        assertEquals(ErrorCode.INVALID_RATING_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("Lấy danh sách candidates trả về đúng thành viên và trạng thái isReviewed")
    void getPeerReviewCandidates_Success() {
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(endedTrip));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, reviewerUserId))
                .thenReturn(Optional.of(reviewerMember));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(reviewerMember, revieweeMember));

        GroupPeerReview existingReview = new GroupPeerReview();
        existingReview.setGroupPeerReviewId(UUID.randomUUID());
        existingReview.setRevieweeMatchingMember(revieweeMember);

        when(groupPeerReviewRepository.findMySubmittedReviews(
                tripId, reviewerUserId)).thenReturn(List.of(existingReview));

        List<PeerReviewCandidateResponse> candidates = peerReviewService.getPeerReviewCandidates(groupId, reviewerUserId);

        assertNotNull(candidates);
        assertEquals(1, candidates.size()); // Loại trừ chính reviewer
        PeerReviewCandidateResponse candidate = candidates.get(0);
        assertEquals(revieweeUserId, candidate.getUserId());
        assertTrue(candidate.isReviewed());
        assertEquals(existingReview.getGroupPeerReviewId(), candidate.getExistingReviewId());
    }

    @Test
    @DisplayName("Lấy danh sách đánh giá nhận được từ bạn đồng hành (findMyReceivedReviews)")
    void getGroupPeerReviews_ReturnsReceivedReviews() {
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(endedTrip));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, reviewerUserId))
                .thenReturn(Optional.of(reviewerMember));

        GroupPeerReview receivedReview = new GroupPeerReview();
        receivedReview.setGroupPeerReviewId(UUID.randomUUID());
        receivedReview.setActualEnduranceRating((short) 5);
        receivedReview.setPunctualityResponsibilityRating((short) 4);
        receivedReview.setFinancialFairnessRating((short) 5);
        receivedReview.setComment("Đồng hành rất vui!");
        receivedReview.setCreatedAt(LocalDateTime.now());
        receivedReview.setRevieweeMatchingMember(reviewerMember);

        when(groupPeerReviewRepository.findMyReceivedReviews(tripId, reviewerUserId))
                .thenReturn(List.of(receivedReview));

        List<PeerReviewResponse> responses = peerReviewService.getGroupPeerReviews(groupId, reviewerUserId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals((short) 5, responses.get(0).getActualEnduranceRating());
        assertEquals("Đồng hành rất vui!", responses.get(0).getComment());
    }

    @Test
    @DisplayName("Lấy danh sách đánh giá công khai ẩn danh của user trên Profile (getUserPeerReviews)")
    void getUserPeerReviews_Success() {
        when(userRepository.findById(revieweeUserId)).thenReturn(Optional.of(revieweeUser));

        GroupPeerReview publicReview = new GroupPeerReview();
        publicReview.setGroupPeerReviewId(UUID.randomUUID());
        publicReview.setActualEnduranceRating((short) 5);
        publicReview.setPunctualityResponsibilityRating((short) 5);
        publicReview.setFinancialFairnessRating((short) 5);
        publicReview.setComment("Thành viên xuất sắc!");
        publicReview.setModerationStatus(PeerReviewModerationStatus.VISIBLE);
        publicReview.setCreatedAt(LocalDateTime.now());
        publicReview.setRevieweeMatchingMember(revieweeMember);

        when(groupPeerReviewRepository.findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                revieweeUserId, PeerReviewModerationStatus.VISIBLE))
                .thenReturn(List.of(publicReview));

        List<PeerReviewResponse> responses = peerReviewService.getUserPeerReviews(revieweeUserId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals((short) 5, responses.get(0).getActualEnduranceRating());
        assertEquals("Thành viên xuất sắc!", responses.get(0).getComment());
    }
}
